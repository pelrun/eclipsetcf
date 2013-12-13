/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tcf.core.AbstractPeer;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ILocator;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.utils.net.IPAddressUtil;
import org.eclipse.tcf.te.tcf.core.peers.Peer;
import org.eclipse.tcf.te.tcf.locator.ScannerRunnable;
import org.eclipse.tcf.te.tcf.locator.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.locator.interfaces.IModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.ITracing;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelUpdateService;
import org.eclipse.tcf.te.tcf.locator.nodes.PeerNode;
import org.eclipse.tcf.te.tcf.locator.nodes.PeerRedirector;


/**
 * Locator listener implementation.
 */
public final class LocatorListener implements ILocator.LocatorListener {
	// Reference to the parent model
	/* default */ final IPeerModel model;

	/**
	 * Constructor.
	 *
	 * @param model The parent locator model. Must not be <code>null</code>.
	 */
	public LocatorListener(IPeerModel model) {
		super();

		Assert.isNotNull(model);
		this.model = model;
	}

	/**
	 * Returns if or if not the given peer is filtered.
	 *
	 * @param peer The peer or <code>null</code>.
	 * @return <code>True</code> if the given peer is filtered, <code>false</code> otherwise.
	 */
	private boolean isFiltered(IPeer peer) {
		boolean filtered = peer == null;
		boolean hideValueAdds = CoreBundleActivator.getScopedPreferences().getBoolean(org.eclipse.tcf.te.tcf.locator.interfaces.preferences.IPreferenceKeys.PREF_HIDE_VALUEADDS);

		if (!filtered) {
			String value = peer.getAttributes().get("ValueAdd"); //$NON-NLS-1$
			boolean isValueAdd = value != null && ("1".equals(value.trim()) || Boolean.parseBoolean(value.trim())); //$NON-NLS-1$

			filtered |= isValueAdd && hideValueAdds;

			filtered |= peer.getName() != null
							&& peer.getName().endsWith("Command Server"); //$NON-NLS-1$
		}

		return filtered;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.ILocator.LocatorListener#peerAdded(org.eclipse.tcf.protocol.IPeer)
	 */
	@Override
	public void peerAdded(IPeer peer) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("LocatorListener.peerAdded( " + (peer != null ? peer.getID() : null) + " )", ITracing.ID_TRACE_LOCATOR_LISTENER, this); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (isFiltered(peer)) return;

		if (model != null && peer != null) {
			// find the corresponding model node to remove (expected to be null)
			IPeerNode peerNode = model.getService(IPeerModelLookupService.class).lkupPeerModelById(peer.getID());
			if (peerNode == null) {
				// Double check with "ClientID" if set
				String clientID = peer.getAttributes().get("ClientID"); //$NON-NLS-1$
				if (clientID != null) {
					peerNode = model.getService(IPeerModelLookupService.class).lkupPeerModelById(clientID);
				}
			}
			// If not found, create a new peer node instance
			if (peerNode == null) {
				peerNode = new PeerNode(model, peer);
				// Validate the peer node before adding
				peerNode = model.validatePeerNodeForAdd(peerNode);
				// Add the peer node to the model
				if (peerNode != null) {
					// If there are reachable static peers without an agent ID associated or
					// static peers with unknown link state, refresh the agent ID's first.
					List<IPeerNode> nodes = new ArrayList<IPeerNode>();

					for (IPeerNode node : model.getPeers()) {
						// We expect the agent ID to be set
						if (node.getPeer().getAgentID() == null || "".equals(node.getPeer().getAgentID())) { //$NON-NLS-1$
							nodes.add(node);
						}
					}

					// Create the runnable to execute after the agent ID refresh (if needed)
					final IPeerNode finPeerNode = peerNode;
					final IPeer finPeer = peer;
					final Runnable runnable = new Runnable() {
						@Override
						public void run() {
							IPeerNode[] matches = model.getService(IPeerModelLookupService.class).lkupMatchingStaticPeerModels(finPeerNode);
							if (matches.length == 0) {
								// If the peer node is still in the model, schedule for immediate status update
								if (model.getService(IPeerModelLookupService.class).lkupPeerModelById(finPeerNode.getPeerId()) != null) {
									Runnable runnable2 = new ScannerRunnable(model.getScanner(), finPeerNode);
									Protocol.invokeLater(runnable2);
								}
							} else {
								// Remove the preliminary added node from the model again
								model.getService(IPeerModelUpdateService.class).remove(finPeerNode);

								for (IPeerNode match : matches) {
									IPeer myPeer = model.validatePeer(finPeer);
									if (myPeer != null) {
										// Update the matching static node
										boolean changed = match.setChangeEventsEnabled(false);
										// Merge user configured properties between the peers
										model.getService(IPeerModelUpdateService.class).mergeUserDefinedAttributes(match, myPeer, true);
										if (changed) match.setChangeEventsEnabled(true);
										match.fireChangeEvent(IPeerNodeProperties.PROP_INSTANCE, myPeer, match.getPeer());
										// And schedule for immediate status update
										Runnable runnable2 = new ScannerRunnable(model.getScanner(), match);
										Protocol.invokeLater(runnable2);
									}
								}
							}
						}
					};

					// Preliminary add the node to the model now. If we have to refresh the agent ID,
					// this is an asynchronous operation and other peerAdded events might be processed before.
					model.getService(IPeerModelUpdateService.class).add(peerNode);

					if (nodes.size() > 0) {
						// Refresh the agent ID's first
						model.getService(IPeerModelRefreshService.class).refreshAgentIDs(nodes.toArray(new IPeerNode[nodes.size()]), new Callback() {
							@Override
							protected void internalDone(Object caller, IStatus status) {
								// Ignore errors
								runnable.run();
							}
						});
					} else {
						// No need to refresh the agent ID's -> run runnable
						runnable.run();
					}
				}
			} else {
				// Peer node found, update the peer instance
					// Validate the peer node before updating
					IPeer myPeer = model.validatePeer(peer);
					if (myPeer != null) {
						boolean changed = peerNode.setChangeEventsEnabled(false);
						// Merge user configured properties between the peers
						model.getService(IPeerModelUpdateService.class).mergeUserDefinedAttributes(peerNode, myPeer, true);
						if (changed) peerNode.setChangeEventsEnabled(true);
						peerNode.fireChangeEvent(IPeerNodeProperties.PROP_INSTANCE, myPeer, peerNode.getPeer());
						// And schedule for immediate status update
						Runnable runnable = new ScannerRunnable(model.getScanner(), peerNode);
						Protocol.invokeLater(runnable);
					}
			}
		}
	}

	// Map of guardian objects per peer
	private final Map<IPeer, AtomicBoolean> PEER_CHANGED_GUARDIANS = new HashMap<IPeer, AtomicBoolean>();

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.ILocator.LocatorListener#peerChanged(org.eclipse.tcf.protocol.IPeer)
	 */
	@Override
	public void peerChanged(IPeer peer) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("LocatorListener.peerChanged( " + (peer != null ? peer.getID() : null) + " )", ITracing.ID_TRACE_LOCATOR_LISTENER, this); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (isFiltered(peer)) return;

		// Protect ourself from reentrant calls while processing a changed peer.
		if (peer != null) {
			AtomicBoolean guard = PEER_CHANGED_GUARDIANS.get(peer);
			if (guard != null && guard.get()) return;
			if (guard != null) guard.set(true);
			else PEER_CHANGED_GUARDIANS.put(peer, new AtomicBoolean(true));
		}

		if (model != null && peer != null) {
			// find the corresponding model node to remove
			IPeerNode peerNode = model.getService(IPeerModelLookupService.class).lkupPeerModelById(peer.getID());
			if (peerNode == null) {
				// Double check with "ClientID" if set
				String clientID = peer.getAttributes().get("ClientID"); //$NON-NLS-1$
				if (clientID != null) {
					peerNode = model.getService(IPeerModelLookupService.class).lkupPeerModelById(clientID);
				}
			}
			// Update the peer instance
			if (peerNode != null) {
			    // Get the old peer instance
			    IPeer oldPeer = peerNode.getPeer();
			    // If old peer and new peer instance are the same _objects_, nothing to do
			    if (oldPeer != peer) {
			    	// Peers visible to the locator are replaced with the new instance
			    	if (oldPeer instanceof AbstractPeer) {
			    		peerNode.setProperty(IPeerNodeProperties.PROP_INSTANCE, peer);
			    	}
			    	// Non-visible peers are updated
			    	else {
						// Validate the peer node before updating
						IPeer myPeer = model.validatePeer(peer);
						if (myPeer != null) {
							boolean changed = peerNode.setChangeEventsEnabled(false);
							// Merge user configured properties between the peers
							model.getService(IPeerModelUpdateService.class).mergeUserDefinedAttributes(peerNode, myPeer, true);
							if (changed) peerNode.setChangeEventsEnabled(true);
							peerNode.fireChangeEvent(IPeerNodeProperties.PROP_INSTANCE, myPeer, peerNode.getPeer());
						}
			    	}
			    }
			}
			// Refresh static peers and merge attributes if required
			model.getService(IPeerModelRefreshService.class).refreshStaticPeers();
		}

		// Clean up the guardians
		if (peer != null) PEER_CHANGED_GUARDIANS.remove(peer);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.ILocator.LocatorListener#peerRemoved(java.lang.String)
	 */
	@Override
	public void peerRemoved(String id) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("LocatorListener.peerRemoved( " + id + " )", ITracing.ID_TRACE_LOCATOR_LISTENER, this); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (model != null && id != null) {
			// find the corresponding model node to remove
			IPeerNode peerNode = model.getService(IPeerModelLookupService.class).lkupPeerModelById(id);

			// If we cannot find a model node, it is probably because the remove is sent for the
			// non-loopback addresses of the localhost. We have to double check this.
			if (peerNode == null) {
				int beginIndex = id.indexOf(':');
				int endIndex = id.lastIndexOf(':');
				String ip = id.substring(beginIndex+1, endIndex);

				// Get the loopback address
				String loopback = IPAddressUtil.getInstance().getIPv4LoopbackAddress();
				// Empty IP address means loopback
				if ("".equals(ip)) ip = loopback; //$NON-NLS-1$
				else {
					if (IPAddressUtil.getInstance().isLocalHost(ip)) {
						ip = loopback;
					}
				}
				// Build up the new id to lookup
				StringBuilder newId = new StringBuilder();
				newId.append(id.substring(0, beginIndex));
				newId.append(':');
				newId.append(ip);
				newId.append(':');
				newId.append(id.substring(endIndex + 1));

				// Try the lookup again
				peerNode = model.getService(IPeerModelLookupService.class).lkupPeerModelById(newId.toString());
			}

			// If the model node is found in the model, process the removal.
			if (peerNode != null) {
					boolean changed = peerNode.setChangeEventsEnabled(false);
					IPeer peer = peerNode.getPeer();

					// Create a modifiable copy of the peer attributes
					Map<String, String> attrs = new HashMap<String, String>(peerNode.getPeer().getAttributes());
					// Remember the remote peer id before removing it
					String remotePeerID = attrs.get("remote.id.transient"); //$NON-NLS-1$

					// Remove all merged attributes from the peer instance
					String merged = attrs.remove("remote.merged.transient"); //$NON-NLS-1$
					if (merged != null) {
						merged = merged.replace('[', ' ').replace(']', ' ').trim();
						List<String> keysToRemove = Arrays.asList(merged.split(",\\ ")); //$NON-NLS-1$
						String[] keys = attrs.keySet().toArray(new String[attrs.keySet().size()]);
						for (String key : keys) {
							if (keysToRemove.contains(key)) {
								attrs.remove(key);
							}
						}

						// Make sure the ID is set correctly
						if (attrs.get(IPeer.ATTR_ID) == null) {
							attrs.put(IPeer.ATTR_ID, peer.getID());
						}

						// Update the peer attributes
						if (peer instanceof PeerRedirector) {
							((PeerRedirector)peer).updateAttributes(attrs);
						} else if (peer instanceof Peer) {
							((Peer)peer).updateAttributes(attrs);
						}
					}

					// Remove the attributes stored at peer node level
					peerNode.setProperty(IPeerNodeProperties.PROP_LOCAL_SERVICES, null);
					peerNode.setProperty(IPeerNodeProperties.PROP_REMOTE_SERVICES, null);

					// Check if we have to remote the peer in the underlying locator service too
					if (remotePeerID != null) {
				        Map<String, IPeer> peers = Protocol.getLocator().getPeers();
				        IPeer remotePeer = peers.get(remotePeerID);
				        if (remotePeer instanceof AbstractPeer) ((AbstractPeer)remotePeer).dispose();
					}

					// Clean out possible child nodes
					peerNode.getModel().setChildren(peerNode.getPeerId(), null);

					if (changed) peerNode.setChangeEventsEnabled(true);
					peerNode.fireChangeEvent(IPeerNodeProperties.PROP_INSTANCE, peer, peerNode.getPeer());

					final IModelListener[] listeners = model.getListener();
					if (listeners.length > 0) {
						final IPeerNode finPeerNode = peerNode;
						Protocol.invokeLater(new Runnable() {
							@Override
							public void run() {
								for (IModelListener listener : listeners) {
									listener.locatorModelChanged(model, finPeerNode, false);
								}
							}
						});
					}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.ILocator.LocatorListener#peerHeartBeat(java.lang.String)
	 */
	@Override
	public void peerHeartBeat(String id) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("LocatorListener.peerHeartBeat( " + id + " )", ITracing.ID_TRACE_LOCATOR_LISTENER, this); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

}
