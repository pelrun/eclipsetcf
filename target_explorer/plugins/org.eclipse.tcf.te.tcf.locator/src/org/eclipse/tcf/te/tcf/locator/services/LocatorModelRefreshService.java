/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.services;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.core.Command;
import org.eclipse.tcf.core.TransientPeer;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ILocator;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx.QueryState;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx.QueryType;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistableNodeProperties;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.core.peers.Peer;
import org.eclipse.tcf.te.tcf.core.util.persistence.PeerDataHelper;
import org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelUpdateService;
import org.eclipse.tcf.te.tcf.locator.model.ModelLocationUtil;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.locator.nodes.LocatorNode;

/**
 * Default locator model refresh service implementation.
 */
public class LocatorModelRefreshService extends AbstractLocatorModelService implements ILocatorModelRefreshService {

	/**
	 * Constructor.
	 *
	 * @param parentModel The parent locator model instance. Must not be <code>null</code>.
	 */
	public LocatorModelRefreshService(ILocatorModel parentModel) {
		super(parentModel);
	}

	/**
	 * Asynchronously invoke the callback within the TCF dispatch thread.
	 *
	 * @param callback The callback to invoke or <code>null</code>.
	 */
	protected final void invokeCallback(final ICallback callback) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (callback != null) {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					callback.done(LocatorModelRefreshService.this, Status.OK_STATUS);
				}
			});
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService#refresh(org.eclipse
	 * .tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void refresh(final ICallback callback) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// Get the parent peer model
		ILocatorModel model = getLocatorModel();

		// If the parent model is already disposed, the service will drop out immediately
		if (model.isDisposed()) {
			invokeCallback(callback);
			return;
		}

		// If the TCF framework isn't initialized yet, the service will drop out immediately
		if (!Tcf.isRunning()) {
			invokeCallback(callback);
			return;
		}

		refreshStaticPeers();

		// Get the list of old children (update node instances where possible)
		ILocatorNode[] locatorNodes = model.getLocatorNodes();
		List<IPeer> oldChildren = new ArrayList<IPeer>();
		for (ILocatorNode node : locatorNodes) {
			if (!node.isStatic()) {
				oldChildren.add(node.getPeer());
			}
		}

		// Refresh the static peer definitions
		processPeers(Protocol.getLocator().getPeers(), oldChildren, model);

		// If there are remaining old children, remove them from the model (non-recursive)
		for (IPeer oldChild : oldChildren) {
			model.getService(ILocatorModelUpdateService.class).remove(oldChild);
		}

		ILocatorModelListener[] listeners = model.getListener();
		for (ILocatorModelListener listener : listeners) {
			listener.modelChanged(model, null, false);
		}

		// Invoke the callback
		invokeCallback(callback);
	}

	/**
	 * Process the given map of peers and update the given locator model.
	 *
	 * @param peers The map of peers to process. Must not be <code>null</code>.
	 * @param oldChildren The list of old children. Must not be <code>null</code>.
	 * @param model The locator model. Must not be <code>null</code>.
	 */
	protected void processPeers(Map<String, IPeer> peers, List<IPeer> oldChildren, ILocatorModel model) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(peers);
		Assert.isNotNull(oldChildren);
		Assert.isNotNull(model);

		for (Entry<String, IPeer> entry : peers.entrySet()) {
			// Get the peer instance for the current peer id
			IPeer peer = entry.getValue();
			// Check if the peer is filtered
			if (isFiltered(peer)) continue;
			// Try to find an existing peer node first
			IPeer lkupPeer = model.getService(ILocatorModelLookupService.class)
			                .lkupPeerById(entry.getKey());
			// And create a new one if we cannot find it
			if (lkupPeer == null) {
				// Validate peer before adding
				lkupPeer = model.validatePeer(peer);
				if (lkupPeer != null) model.getService(ILocatorModelUpdateService.class)
				                .add(lkupPeer);
			}
			else {
				oldChildren.remove(peer);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelRefreshService#refresh(org
	 * .eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode,
	 * org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void refresh(final ILocatorNode locatorNode, ICallback callback) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// Get the parent peer model
		final ILocatorModel model = getLocatorModel();

		// If the parent model is already disposed, the service will drop out immediately
		if (model.isDisposed()) {
			invokeCallback(callback);
			return;
		}

		// If the TCF framework isn't initialized yet, the service will drop out immediately
		if (!Tcf.isRunning()) {
			invokeCallback(callback);
			return;
		}

		final IAsyncRefreshableCtx refreshCtx = (IAsyncRefreshableCtx) locatorNode
		                .getAdapter(IAsyncRefreshableCtx.class);
		refreshCtx.setQueryState(QueryType.CONTEXT, QueryState.IN_PROGRESS);
		refreshCtx.setQueryState(QueryType.CHILD_LIST, QueryState.IN_PROGRESS);

		Map<String, Boolean> flags = new HashMap<String, Boolean>();
		flags.put(IChannelManager.FLAG_FORCE_NEW, Boolean.TRUE);
		flags.put(IChannelManager.FLAG_NO_PATH_MAP, Boolean.TRUE);
		flags.put(IChannelManager.FLAG_NO_VALUE_ADD, Boolean.TRUE);

		final ICallback finCb = new Callback(callback) {
			@Override
			protected void internalDone(Object caller, IStatus status) {
				refreshCtx.setQueryState(QueryType.CONTEXT, QueryState.DONE);
				refreshCtx.setQueryState(QueryType.CHILD_LIST, QueryState.DONE);
				final ILocatorModel model = ModelManager.getLocatorModel();
				final ILocatorModelListener[] listeners = model.getListener();
				if (listeners.length > 0) {
					Protocol.invokeLater(new Runnable() {
						@Override
						public void run() {
							for (ILocatorModelListener listener : listeners) {
								listener.modelChanged(model, locatorNode, false);
							}
						}
					});
				}
			}
		};

		Tcf.getChannelManager().openChannel(locatorNode.getPeer(), flags, new IChannelManager.DoneOpenChannel() {
            @Override
            public void doneOpenChannel(Throwable error, final IChannel channel) {
                if (error != null || channel == null) {
	                locatorNode.removeAll(ILocatorNode.class);
	                refreshCtx.setQueryState(QueryType.CONTEXT, QueryState.DONE);
	                refreshCtx.setQueryState(QueryType.CHILD_LIST, QueryState.DONE);
	                if (channel != null) {
		                Tcf.getChannelManager().closeChannel(channel);
	                }
	                if (locatorNode.isStatic()) {
	                	locatorNode.setProperty(IPeerNodeProperties.PROPERTY_INSTANCE, locatorNode.getProperty(ILocatorNode.PROPERTY_STATIC_INSTANCE));
	                }
	                else {
	                	ILocatorModelUpdateService update = model.getService(ILocatorModelUpdateService.class);
	                	update.remove(locatorNode.getPeer());
	                }
	                invokeCallback(finCb);
                }
                else {
	                onDoneOpenChannelRefreshLocatorNode(channel, locatorNode, new Callback(finCb) {
		                @Override
		                protected void internalDone(Object caller, org.eclipse.core.runtime.IStatus status) {
			                Tcf.getChannelManager().closeChannel(channel);
		                }
	                });
                }
            }
        });
	}

	protected void onDoneOpenChannelRefreshLocatorNode(final IChannel channel, final ILocatorNode locatorNode, final ICallback callback) {
		final ILocator locator = channel.getRemoteService(ILocator.class);
		final IAsyncRefreshableCtx refreshCtx = (IAsyncRefreshableCtx) locatorNode
		                .getAdapter(IAsyncRefreshableCtx.class);
		if (locator == null) {
			locatorNode.removeAll(ILocatorNode.class);
			refreshCtx.setQueryState(QueryType.CONTEXT, QueryState.DONE);
			refreshCtx.setQueryState(QueryType.CHILD_LIST, QueryState.DONE);
			invokeCallback(callback);
		}
		else {
			locator.getAgentID(new ILocator.DoneGetAgentID() {
				@Override
				public void doneGetAgentID(IToken token, Exception error, String agentID) {
					if (error == null) {
						locatorNode.setProperty(IPeer.ATTR_AGENT_ID, agentID);
					}
					refreshCtx.setQueryState(QueryType.CONTEXT, QueryState.DONE);
					getPeers(locator, channel, locatorNode, callback);
				}
			});
		}
	}

	protected void getPeers(final ILocator locator, final IChannel channel, final ILocatorNode locatorNode, final ICallback callback) {
		@SuppressWarnings("unused")
		Command cmd = new Command(channel, locator, "getPeers", null) { //$NON-NLS-1$
			@Override
			public void done(Exception error, Object[] args) {
				if (error == null) {
					Assert.isTrue(args.length == 2);
					error = toError(args[0]);
				}
				// If the error is still null here, process the returned peers
				if (error == null && args[1] != null) {
					// calculate proxies
					String parentProxies = locatorNode.getPeer().getAttributes()
					                .get(IPeerProperties.PROP_PROXIES);
					IPeer[] proxies = PeerDataHelper.decodePeerList(parentProxies);
					List<IPeer> proxiesList = new ArrayList<IPeer>(Arrays.asList(proxies));
					proxiesList.add(locatorNode.getPeer());
					proxies = proxiesList.toArray(new IPeer[proxiesList.size()]);
					String encProxies = PeerDataHelper.encodePeerList(proxies);

					List<ILocatorNode> oldChildren = locatorNode.getChildren(ILocatorNode.class);

					ILocatorModelLookupService lkup = getLocatorModel()
					                .getService(ILocatorModelLookupService.class);

					String parentAgentId = locatorNode.getPeer().getAgentID();
					if (parentAgentId == null) {
						parentAgentId = locatorNode.getStringProperty(IPeer.ATTR_AGENT_ID);
					}
					String parentId = normalizeId(locatorNode.getPeer().getID());

					@SuppressWarnings("unchecked")
					Collection<Map<String, String>> peerAttributesList = (Collection<Map<String, String>>) args[1];
					for (Map<String, String> attributes : peerAttributesList) {

						String agentId = attributes.get(IPeer.ATTR_AGENT_ID);
						String id = attributes.get(IPeer.ATTR_ID);
						String normalizedId = normalizeId(id);
						ILocatorNode existing = null;
						ILocatorNode[] lkupNodes = agentId != null ? lkup
						                .lkupLocatorNodeByAgentId(locatorNode, agentId) : new ILocatorNode[0];
						if (lkupNodes.length == 0) {
							lkupNodes = id != null ? lkup.lkupLocatorNodeById(locatorNode, id) : new ILocatorNode[0];
						}
						for (ILocatorNode node : lkupNodes) {
							if (normalizeId(node.getPeer().getID()).equals(normalizedId)) {
								oldChildren.remove(node);
								existing = node;
								break;
							}
						}

						if (agentId != null && !agentId.equals(parentAgentId)) {
							ILocatorNode parent = locatorNode.getParent(ILocatorNode.class);
							ILocatorNode[] parentNodes = lkup
							                .lkupLocatorNodeByAgentId(parent, agentId);
							while (parentNodes.length == 0 && parent != null) {
								parent = parent.getParent(ILocatorNode.class);
								parentNodes = lkup.lkupLocatorNodeByAgentId(parent, agentId);
							}

							attributes = new HashMap<String, String>(attributes);
							attributes.put(IPeerProperties.PROP_PROXIES, encProxies);
							IPeer peer = new TransientPeer(attributes);

							if (existing == null) {
								if (parentNodes.length == 0 && !isFiltered(peer)) {
									locatorNode.add(new LocatorNode(peer));
								}
							}
							else {
								if (parentNodes.length == 0 && !isFiltered(peer)) {
									existing.setProperty(IPeerNodeProperties.PROPERTY_INSTANCE, peer);
								}
								else {
									locatorNode.remove(existing, true);
								}
							}
						}
						else if (locatorNode.isStatic()) {
							attributes = new HashMap<String, String>(attributes);
							attributes.put(IPeerProperties.PROP_PROXIES, parentProxies);
							attributes.putAll(((IPeer)locatorNode.getProperty(ILocatorNode.PROPERTY_STATIC_INSTANCE)).getAttributes());
							IPeer peer = new TransientPeer(attributes);
							locatorNode.setProperty(IPeerNodeProperties.PROPERTY_INSTANCE, peer);
						}
					}
					for (ILocatorNode old : oldChildren) {
						if (!old.isStatic()) {
							locatorNode.remove(old, true);
						}
					}
					for (ILocatorNode child : locatorNode.getChildren(ILocatorNode.class)) {
						String childAgentId = child.getPeer().getAgentID();
						String childId = child.getPeer().getID();
						if (parentAgentId.equals(childAgentId)) {
							locatorNode.remove(child, true);
						}
						else if (parentId != null) {
							if (parentId.equals(normalizeId(childId))) {
								locatorNode.remove(child, true);
							}
						}
					}
				}
				else {
					List<ILocatorNode> oldChildren = locatorNode.getChildren(ILocatorNode.class);
					for (ILocatorNode old : oldChildren) {
						if (old.isStatic()) {
							old.setProperty(IPeerNodeProperties.PROPERTY_INSTANCE, locatorNode
							                .getProperty(ILocatorNode.PROPERTY_STATIC_INSTANCE));
						}
						else {
							locatorNode.remove(old, true);
						}
					}
				}
				IAsyncRefreshableCtx refreshCtx = (IAsyncRefreshableCtx) locatorNode
				                .getAdapter(IAsyncRefreshableCtx.class);
				refreshCtx.setQueryState(QueryType.CHILD_LIST, QueryState.DONE);

				// Invoke the callback
				invokeCallback(callback);
			}
		};

	}

	protected String normalizeId(String id) {
		if (id != null) {
			id = id.toLowerCase().replaceAll(":localhost:", "::"); //$NON-NLS-1$ //$NON-NLS-2$
			id = id.replaceAll(":127\\.0+\\.0+\\.0*1:", "::"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return id;
	}

	/**
	 * Returns if or if not the given peer is filtered.
	 *
	 * @param peer The peer or <code>null</code>.
	 * @return <code>True</code> if the given peer is filtered, <code>false</code> otherwise.
	 */
	protected boolean isFiltered(IPeer peer) {
		boolean filtered = peer == null;

		if (!filtered) {
			String value = peer.getAttributes().get("ValueAdd"); //$NON-NLS-1$
			boolean isValueAdd = value != null && ("1".equals(value.trim()) || Boolean.parseBoolean(value.trim())); //$NON-NLS-1$

			boolean isCli = peer.getName() != null && (peer.getName().endsWith("Command Server") || peer.getName().endsWith("CLI Server")); //$NON-NLS-1$ //$NON-NLS-2$

			filtered = isValueAdd || isCli;
		}

		return filtered;
	}

	/**
	 * Returns the list of root locations to lookup for static locator definitions.
	 *
	 * @return The list of root locations or an empty list.
	 */
	protected File[] getStaticLocatorsLookupDirectories() {
		// The list defining the root locations
		List<File> rootLocations = new ArrayList<File>();

		// always add default root location
		IPath defaultPath = ModelLocationUtil.getStaticLocatorsRootLocation();
		if (defaultPath != null) {
			File file = defaultPath.toFile();
			if (file.canRead() && file.isDirectory() && !rootLocations.contains(file)) {
				rootLocations.add(file);
			}
		}

		return rootLocations.toArray(new File[rootLocations.size()]);
	}

	/**
	 * Refresh the static locator definitions.
	 */
	protected void refreshStaticPeers() {

		// Get the root locations to lookup the static peer definitions
		File[] roots = getStaticLocatorsLookupDirectories();
		if (roots.length > 0) {
			// The lst of locator peers created from the static definitions
			List<IPeer> peers = new ArrayList<IPeer>();
			// Process the root locations
			for (File root : roots) {
				// List all "*.locator" files within the root location
				File[] candidates = root.listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						IPath path = new Path(pathname.getAbsolutePath());
						return path.getFileExtension() != null && path.getFileExtension()
						                .toLowerCase().equals("locator"); //$NON-NLS-1$
					}
				});
				// If there are ini files to read, process them
				if (candidates != null && candidates.length > 0) {

					for (File candidate : candidates) {
						try {
							IURIPersistenceService service = ServiceManager.getInstance()
							                .getService(IURIPersistenceService.class);
							IPeer tempPeer = (IPeer) service.read(IPeer.class, candidate
							                .getAbsoluteFile().toURI());
							Map<String, String> attrs = new HashMap<String, String>(tempPeer.getAttributes());
							// Remember the file path within the properties
							attrs.put(IPersistableNodeProperties.PROPERTY_URI, candidate
							                .getAbsoluteFile().toURI().toString());
							// Construct the peer from the attributes
							IPeer peer = new Peer(attrs);
							peers.add(peer);
						}
						catch (IOException e) {
							/* ignored on purpose */
						}
					}
				}
			}

			for (IPeer peer : peers) {
				ILocatorModelUpdateService update = getLocatorModel()
				                .getService(ILocatorModelUpdateService.class);
				update.add(peer, true);
			}
		}
	}
}
