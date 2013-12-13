/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ILocator;
import org.eclipse.tcf.te.runtime.utils.net.IPAddressUtil;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.listeners.interfaces.IChannelStateChangeListener;
import org.eclipse.tcf.te.tcf.locator.Scanner;
import org.eclipse.tcf.te.tcf.locator.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.locator.interfaces.IModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.IScanner;
import org.eclipse.tcf.te.tcf.locator.interfaces.ITracing;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelQueryService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelUpdateService;
import org.eclipse.tcf.te.tcf.locator.listener.ChannelStateChangeListener;
import org.eclipse.tcf.te.tcf.locator.listener.LocatorListener;
import org.eclipse.tcf.te.tcf.locator.services.PeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.services.PeerModelQueryService;
import org.eclipse.tcf.te.tcf.locator.services.PeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.services.PeerModelUpdateService;


/**
 * Default locator model implementation.
 */
public class PeerModel extends PlatformObject implements IPeerModel {
	// The unique model id
	private final UUID uniqueId = UUID.randomUUID();
	// Flag to mark the model disposed
	private boolean disposed;

	// The list of known peers
	/* default */ final Map<String, IPeerNode> peers = new HashMap<String, IPeerNode>();
	// The list of "proxied" peers per proxy peer id
	/* default */ final Map<String, List<IPeerNode>> peerChildren = new HashMap<String, List<IPeerNode>>();

	// Reference to the scanner
	private IScanner scanner = null;

	// Reference to the model locator listener
	private ILocator.LocatorListener locatorListener = null;
	// Reference to the model channel state change listener
	private IChannelStateChangeListener channelStateChangeListener = null;

	// The list of registered model listeners
	private final List<IModelListener> modelListener = new ArrayList<IModelListener>();

	// Reference to the refresh service
	private final IPeerModelRefreshService refreshService = new PeerModelRefreshService(this);
	// Reference to the lookup service
	private final IPeerModelLookupService lookupService = new PeerModelLookupService(this);
	// Reference to the update service
	private final IPeerModelUpdateService updateService = new PeerModelUpdateService(this);
	// Reference to the query service
	private final IPeerModelQueryService queryService = new PeerModelQueryService(this);

	/**
	 * Constructor.
	 */
	public PeerModel() {
		super();
		disposed = false;

		channelStateChangeListener = new ChannelStateChangeListener(this);
		Tcf.addChannelStateChangeListener(channelStateChangeListener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#addListener(org.eclipse.tcf.te.tcf.locator.core.interfaces.IModelListener)
	 */
	@Override
	public void addListener(IModelListener listener) {
		Assert.isNotNull(listener);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("PeerModel.addListener( " + listener + " )", ITracing.ID_TRACE_LOCATOR_MODEL, this); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (!modelListener.contains(listener)) modelListener.add(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#removeListener(org.eclipse.tcf.te.tcf.locator.core.interfaces.IModelListener)
	 */
	@Override
	public void removeListener(IModelListener listener) {
		Assert.isNotNull(listener);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("PeerModel.removeListener( " + listener + " )", ITracing.ID_TRACE_LOCATOR_MODEL, this); //$NON-NLS-1$ //$NON-NLS-2$
		}

		modelListener.remove(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel#getListener()
	 */
	@Override
	public IModelListener[] getListener() {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		return modelListener.toArray(new IModelListener[modelListener.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#dispose()
	 */
	@Override
	public void dispose() {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("PeerModel.dispose()", ITracing.ID_TRACE_LOCATOR_MODEL, this); //$NON-NLS-1$
		}

		// If already disposed, we are done immediately
		if (disposed) return;

		disposed = true;

		final IModelListener[] listeners = getListener();
		if (listeners.length > 0) {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					for (IModelListener listener : listeners) {
						listener.locatorModelDisposed(PeerModel.this);
					}
				}
			});
		}
		modelListener.clear();

		if (locatorListener != null) {
			Protocol.getLocator().removeListener(locatorListener);
			locatorListener = null;
		}

		if (channelStateChangeListener != null) {
			Tcf.removeChannelStateChangeListener(channelStateChangeListener);
			channelStateChangeListener = null;
		}

		if (scanner != null) {
			stopScanner();
			scanner = null;
		}

		peers.clear();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#isDisposed()
	 */
	@Override
	public boolean isDisposed() {
		return disposed;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#getPeers()
	 */
	@Override
	public IPeerNode[] getPeers() {
		final AtomicReference<IPeerNode[]> result = new AtomicReference<IPeerNode[]>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				result.set(peers.values().toArray(new IPeerNode[peers.values().size()]));
			}
		};

		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeAndWait(runnable);

		return result.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel#getChildren(java.lang.String)
	 */
	@Override
    public List<IPeerNode> getChildren(final String parentPeerID) {
		Assert.isNotNull(parentPeerID);

		final AtomicReference<List<IPeerNode>> result = new AtomicReference<List<IPeerNode>>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				List<IPeerNode> children = peerChildren.get(parentPeerID);
				if (children == null) children = Collections.emptyList();
				result.set(children);
			}
		};

		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeAndWait(runnable);

		return Collections.unmodifiableList(result.get());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel#setChildren(java.lang.String, java.util.List)
	 */
	@Override
    public void setChildren(String parentPeerID, List<IPeerNode> children) {
		Assert.isNotNull(parentPeerID);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (children == null || children.size() == 0) {
			peerChildren.remove(parentPeerID);
		} else {
			peerChildren.put(parentPeerID, new ArrayList<IPeerNode>(children));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.isAssignableFrom(ILocator.LocatorListener.class)) {
			return locatorListener;
		}
		if (adapter.isAssignableFrom(IScanner.class)) {
			return scanner;
		}
		if (adapter.isAssignableFrom(IPeerModelRefreshService.class)) {
			return refreshService;
		}
		if (adapter.isAssignableFrom(IPeerModelLookupService.class)) {
			return lookupService;
		}
		if (adapter.isAssignableFrom(IPeerModelUpdateService.class)) {
			return updateService;
		}
		if (adapter.isAssignableFrom(IPeerModelQueryService.class)) {
			return queryService;
		}
		if (adapter.isAssignableFrom(Map.class)) {
			return peers;
		}

		return super.getAdapter(adapter);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
	    return uniqueId.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof PeerModel) {
			return uniqueId.equals(((PeerModel)obj).uniqueId);
		}
		return super.equals(obj);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#getService(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <V extends IPeerModelService> V getService(Class<V> serviceInterface) {
		Assert.isNotNull(serviceInterface);
		return (V)getAdapter(serviceInterface);
	}

	/**
	 * Check if the locator listener has been created and registered
	 * to the global locator service.
	 * <p>
	 * <b>Note:</b> This method is not intended to be call from clients.
	 */
	public void checkLocatorListener() {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(Protocol.getLocator());

		if (locatorListener == null) {
			locatorListener = doCreateLocatorListener(this);
			Protocol.getLocator().addListener(locatorListener);
		}
	}

	/**
	 * Creates the locator listener instance.
	 *
	 * @param model The parent model. Must not be <code>null</code>.
	 * @return The locator listener instance.
	 */
	protected ILocator.LocatorListener doCreateLocatorListener(IPeerModel model) {
		Assert.isNotNull(model);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		return new LocatorListener(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#getScanner()
	 */
	@Override
	public IScanner getScanner() {
		if (scanner == null) scanner = new Scanner(this);
		return scanner;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#startScanner(long, long)
	 */
	@Override
	public void startScanner(long delay, long schedule) {
		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("PeerModel.startScanner( " + delay + ", " + schedule + " )", ITracing.ID_TRACE_LOCATOR_MODEL, this); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		IScanner scanner = getScanner();
		Assert.isNotNull(scanner);

		// Pass on the schedule parameter
		Map<String, Object> config = new HashMap<String, Object>(scanner.getConfiguration());
		config.put(IScanner.PROP_SCHEDULE, Long.valueOf(schedule));
		scanner.setConfiguration(config);

		// The default scanner implementation is a job.
		// -> schedule here if it is a job
		if (scanner instanceof Job) {
			Job job = (Job)scanner;
			job.setSystem(true);
			job.setPriority(Job.DECORATE);
			job.schedule(delay);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#stopScanner()
	 */
	@Override
	public void stopScanner() {
		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("PeerModel.stopScanner()", ITracing.ID_TRACE_LOCATOR_MODEL, this); //$NON-NLS-1$
		}

		if (scanner != null) {
			// Terminate the scanner
			scanner.terminate();
			// Reset the scanner reference
			scanner = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel#validatePeer(org.eclipse.tcf.protocol.IPeer)
	 */
	@Override
	public IPeer validatePeer(IPeer peer) {
		Assert.isNotNull(peer);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("PeerModel.validatePeer( " + peer.getID() + " )", ITracing.ID_TRACE_LOCATOR_MODEL, this); //$NON-NLS-1$ //$NON-NLS-2$
		}

		IPeer result = peer;

		// Get the loopback address
		String loopback = IPAddressUtil.getInstance().getIPv4LoopbackAddress();
		// Get the peer IP
		String peerIP = peer.getAttributes().get(IPeer.ATTR_IP_HOST);

		// If the peer node is for local host, we ignore all peers not being
		// associated with the loopback address.
		if (IPAddressUtil.getInstance().isLocalHost(peerIP) && !loopback.equals(peerIP)) {
			// Not loopback address -> drop the peer
			result = null;

			if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
				CoreBundleActivator.getTraceHandler().trace("PeerModel.validatePeer: local host peer but not loopback address -> peer node dropped" //$NON-NLS-1$
															, ITracing.ID_TRACE_LOCATOR_MODEL, this);
			}
		}

	    return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#validatePeerNodeForAdd(org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.IPeerModel)
	 */
	@Override
	public IPeerNode validatePeerNodeForAdd(IPeerNode node) {
		Assert.isNotNull(node);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// Get the peer from the peer node
		IPeer peer = node.getPeer();
		if (peer == null) return node;

		// Skip static peer IP address validation
		return node;

//		// Skip validation if the transport type is not TCP or SSL
//		String transport = peer.getTransportName();
//		if (transport == null || !"TCP".equals(transport) && !"SSL".equals(transport)){ //$NON-NLS-1$ //$NON-NLS-2$
//			return node;
//		}
//
//		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
//			CoreBundleActivator.getTraceHandler().trace("PeerModel.validatePeerNodeForAdd( " + peer.getID() + " )", ITracing.ID_TRACE_LOCATOR_MODEL, this); //$NON-NLS-1$ //$NON-NLS-2$
//		}
//
//		IPeerNode result = node;
//
//		// Get the loopback address
//		String loopback = IPAddressUtil.getInstance().getIPv4LoopbackAddress();
//		// Get the peer IP
//		String peerIP = peer.getAttributes().get(IPeer.ATTR_IP_HOST);
//
//		// If the peer node is for local host, we ignore all peers not being
//		// associated with the loopback address.
//		if (IPAddressUtil.getInstance().isLocalHost(peerIP) && !loopback.equals(peerIP)) {
//			boolean drop = true;
//
//			// Simulator nodes appears on local host IP addresses too, but does not have
//			// a loopback peer available. We have to check the agent ID to determine if
//			// a specific node can be dropped
//			String agentID = peer.getAgentID();
//			if (agentID != null) {
//				// Get all discovered peers
//				Map<String, IPeer> peers = Protocol.getLocator().getPeers();
//				// Sort them by agent id
//				Map<String, List<IPeer>> byAgentID = new HashMap<String, List<IPeer>>();
//
//				for (IPeer candidate : peers.values()) {
//					if (candidate.getAgentID() == null) continue;
//
//					List<IPeer> l = byAgentID.get(candidate.getAgentID());
//					if (l == null) {
//						l = new ArrayList<IPeer>();
//						byAgentID.put(candidate.getAgentID(), l);
//					}
//					Assert.isNotNull(l);
//					if (!l.contains(candidate)) l.add(candidate);
//				}
//
//				// Check all peers found for the same agent ID as the current peer to validate
//				List<IPeer> candidates = byAgentID.get(agentID);
//				if (candidates != null && candidates.size() > 1) {
//					// Check if the found peers contains one with the loopback address
//					drop = false;
//					for (IPeer candidate : candidates) {
//						String ip = candidate.getAttributes().get(IPeer.ATTR_IP_HOST);
//						if (IPAddressUtil.getInstance().isLocalHost(ip) && loopback.equals(ip)) {
//							drop = true;
//							break;
//						}
//					}
//				} else {
//					// No other node for this agent ID -> do not drop the peer
//					drop = false;
//				}
//			}
//
//
//			if (drop) {
//				// Not loopback address -> drop the peer
//				result = null;
//
//				if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
//					CoreBundleActivator.getTraceHandler().trace("PeerModel.validatePeerNodeForAdd: local host peer but not loopback address -> peer node dropped", //$NON-NLS-1$
//																ITracing.ID_TRACE_LOCATOR_MODEL, this);
//				}
//			}
//		}
//
//		// Continue filtering if the node is not yet dropped
//		if (result != null) {
//			List<IPeerNode> previousNodes = new ArrayList<IPeerNode>();
//
//			// Peers are filtered by agent id. Don't add the peer node if we have another peer
//			// node already having the same agent id
//			String agentId = peer.getAgentID();
//			if (agentId != null) {
//				previousNodes.addAll(Arrays.asList(getService(IPeerModelLookupService.class).lkupPeerModelByAgentId(agentId)));
//			}
//
//			// Lookup for matching static peer nodes not found by the agent id lookup
//			IPeerNode[] candidates = getService(IPeerModelLookupService.class).lkupMatchingStaticPeerModels(peer);
//			for (IPeerNode candidate : candidates) {
//				if (!previousNodes.contains(candidate)) previousNodes.add(candidate);
//			}
//
//			if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
//				CoreBundleActivator.getTraceHandler().trace("PeerModel.validatePeerNodeForAdd: agentId=" + agentId + ", Matching peer nodes " //$NON-NLS-1$ //$NON-NLS-2$
//															+ (previousNodes.size() > 0 ? "found (" + previousNodes.size() +")" : "not found --> DONE") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//															, ITracing.ID_TRACE_LOCATOR_MODEL, this);
//			}
//
//			for (IPeerNode previousNode : previousNodes) {
//				// Get the peer for the previous node
//				IPeer previousPeer = previousNode.getPeer();
//				if (previousPeer != null) {
//					// Get the IP address of the previous node
//					String previousPeerIP = previousPeer.getAttributes().get(IPeer.ATTR_IP_HOST);
//					if (IPAddressUtil.getInstance().isLocalHost(previousPeerIP) && !loopback.equals(previousPeerIP) && loopback.equals(peerIP)) {
//						// Remove the previous node from the model
//						getService(IPeerModelUpdateService.class).remove(previousNode);
//
//						if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
//							CoreBundleActivator.getTraceHandler().trace("PeerModel.validatePeerNodeForAdd: Previous peer removed and replaced by new peer representing the loopback address" //$NON-NLS-1$
//											, ITracing.ID_TRACE_LOCATOR_MODEL, this);
//						}
//
//						continue;
//					}
//
//					// Get the ports
//					String peerPort = peer.getAttributes().get(IPeer.ATTR_IP_PORT);
//					String previousPeerPort = previousPeer.getAttributes().get(IPeer.ATTR_IP_PORT);
//
//					if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
//						CoreBundleActivator.getTraceHandler().trace("PeerModel.validatePeerNodeForAdd: peerIP=" + peerIP //$NON-NLS-1$
//										+ ", peerPort=" + peerPort + ", previousPeerPort=" + previousPeerPort //$NON-NLS-1$ //$NON-NLS-2$
//										, ITracing.ID_TRACE_LOCATOR_MODEL, this);
//					}
//
//					// If the ports of the agent instances are identical,
//					// than try to find the best representation of the agent instance
//					if (peerPort != null && peerPort.equals(previousPeerPort))  {
//						// Drop the current node
//						result = null;
//
//						if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
//							CoreBundleActivator.getTraceHandler().trace("PeerModel.validatePeerNodeForAdd: Previous peer node kept, new peer node dropped" //$NON-NLS-1$
//											, ITracing.ID_TRACE_LOCATOR_MODEL, this);
//						}
//
//
//						// Break the loop if the ports matched
//						break;
//					}
//
//					if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
//						CoreBundleActivator.getTraceHandler().trace("PeerModel.validatePeerNodeForAdd: Previous peer node kept, new peer node added (Port mismatch)" //$NON-NLS-1$
//										, ITracing.ID_TRACE_LOCATOR_MODEL, this);
//					}
//				}
//			}
//		}
//
//		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel#validateChildPeerNodeForAdd(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode)
	 */
	@Override
	public IPeerNode validateChildPeerNodeForAdd(final IPeerNode node) {
		Assert.isNotNull(node);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("PeerModel.validateChildPeerNodeForAdd( " + node.getPeerId() + " )", ITracing.ID_TRACE_LOCATOR_MODEL, this); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// Determine the parent node. If null, the child node is invalid
		// and cannot be added
		final IPeerNode parent = node.getParent(IPeerNode.class);
		if (parent == null) return null;

		return validateChildPeerNodeForAdd(parent, node);
	}

	/**
	 * Validates the given child peer model node in relation to the given parent peer model node
	 * hierarchy.
	 * <p>
	 * The method is recursive.
	 *
	 * @param parent The parent model node. Must not be <code>null</code>.
	 * @param node The child model node. Must not be <code>null</code>.
	 *
	 * @return The validated child peer model node, or <code>null</code>.
	 */
	protected IPeerNode validateChildPeerNodeForAdd(IPeerNode parent, IPeerNode node) {
		Assert.isNotNull(parent);
		Assert.isNotNull(node);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("PeerModel.validateChildPeerNodeForAdd( " + parent.getPeerId() + ", " + node.getPeerId() + " )", ITracing.ID_TRACE_LOCATOR_MODEL, this); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		// Validate against the given parent
		if (doValidateChildPeerNodeForAdd(parent, node) == null) {
			return null;
		}

		// If the parent node is child node by itself, validate the
		// child node against the parent parent node.
		if (parent.getParent(IPeerNode.class) != null) {
			IPeerNode parentParentNode = parent.getParent(IPeerNode.class);
			if (doValidateChildPeerNodeForAdd(parentParentNode, node) == null) {
				return null;
			}

			// And validate the child node against all child nodes of the parent parent.
			List<IPeerNode> childrenList = getChildren(parentParentNode.getPeerId());
			IPeerNode[] children = childrenList.toArray(new IPeerNode[childrenList.size()]);
			for (IPeerNode parentParentChild : children) {
				if (node.equals(parentParentChild) || parent.equals(parentParentChild)) {
					return null;
				}
				if (doValidateChildPeerNodeForAdd(parentParentChild, node) == null) {
					return null;
				}
			}
		}

		return node;
	}

	/**
	 * Validates the given child peer model node in relation to the given parent peer model node.
	 * <p>
	 * The method is non-recursive.
	 *
	 * @param parent The parent model node. Must not be <code>null</code>.
	 * @param node The child model node. Must not be <code>null</code>.
	 *
	 * @return The validated child peer model node, or <code>null</code>.
	 */
	protected IPeerNode doValidateChildPeerNodeForAdd(IPeerNode parent, IPeerNode node) {
		Assert.isNotNull(parent);
		Assert.isNotNull(node);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("PeerModel.doValidateChildPeerNodeForAdd( " + parent.getPeerId() + ", " + node.getPeerId() + " )", ITracing.ID_TRACE_LOCATOR_MODEL, this); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		// If the child node is already visible as root node, drop the child node
		String id = node.getPeerId();
		if (isRootNode(id)) {
			return null;
		}

		int beginIndex = id.indexOf(':');
		int endIndex = id.lastIndexOf(':');
		String ip = beginIndex != -1 && endIndex != -1 ? id.substring(beginIndex+1, endIndex) : ""; //$NON-NLS-1$

		// Get the loopback address
		String loopback = IPAddressUtil.getInstance().getIPv4LoopbackAddress();
		// Empty IP address means loopback
		if ("".equals(ip)) ip = loopback; //$NON-NLS-1$

		// If the IP is a localhost IP, try the loopback IP
		if (IPAddressUtil.getInstance().isLocalHost(ip)) {
			// Build up the new id to lookup
			StringBuilder newId = new StringBuilder();
			newId.append(id.substring(0, beginIndex));
			newId.append(':');
			newId.append(loopback);
			newId.append(':');
			newId.append(id.substring(endIndex + 1));

			// Try the lookup again
			if (isRootNode(newId.toString())) {
				return null;
			}
		}

		// Get the peer from the peer node
		IPeer peer = node.getPeer();

		// If the child peer represents the same agent as the parent peer,
		// drop the child peer
		String parentAgentID = parent.getPeer().getAgentID();
		if (parentAgentID != null && parentAgentID.equals(peer.getAgentID())) {
			return null;
		}
		// If the child peer represents the same agent as another child peer,
		// drop the child peer
		String agentID = node.getPeer().getAgentID();
		if (agentID != null) {
			IPeerNode[] matches = getService(IPeerModelLookupService.class).lkupPeerModelByAgentId(parent.getPeerId(), agentID);
			for (IPeerNode match : matches) {
				if (agentID.equals(match.getPeer().getAgentID())) {
					// Try to keep the peer with the real IP, filter the "127.0.0.1" peer
					if ("127.0.0.1".equals(node.getPeer().getAttributes().get(IPeer.ATTR_IP_HOST)) //$NON-NLS-1$
							&& !"127.0.0.1".equals(match.getPeer().getAttributes().get(IPeer.ATTR_IP_HOST))) { //$NON-NLS-1$
						// Keep the other child node
						return null;
					}

					if (!"127.0.0.1".equals(node.getPeer().getAttributes().get(IPeer.ATTR_IP_HOST)) //$NON-NLS-1$
							&& "127.0.0.1".equals(match.getPeer().getAttributes().get(IPeer.ATTR_IP_HOST))) { //$NON-NLS-1$
						// Keep the node
						getService(IPeerModelUpdateService.class).removeChild(match);
					}

					// If both nodes have a IP different from "127.0.0.1", keep the first node
					if (!"127.0.0.1".equals(node.getPeer().getAttributes().get(IPeer.ATTR_IP_HOST)) //$NON-NLS-1$
							&& !"127.0.0.1".equals(match.getPeer().getAttributes().get(IPeer.ATTR_IP_HOST))) { //$NON-NLS-1$
						// Keep the other child node
						return null;
					}
				}
			}
		}
		// If the child peer's IP address and port are the same as the parent's
		// IP address and port, drop the child node
		Map<String, String> parentPeerAttributes = parent.getPeer().getAttributes();
		if (parentPeerAttributes.get(IPeer.ATTR_IP_HOST) != null && parentPeerAttributes.get(IPeer.ATTR_IP_HOST).equals(peer.getAttributes().get(IPeer.ATTR_IP_HOST))) {
			String parentPort = parentPeerAttributes.get(IPeer.ATTR_IP_PORT);
			String port = peer.getAttributes().get(IPeer.ATTR_IP_PORT);

			if (parentPort != null && parentPort.equals(port)) return null;
		}

		return node;
	}

	/**
	 * Checks if the given peer id belongs to an already known root node
	 * or to one of the discovered nodes.
	 *
	 * @param id The peer id. Must not be <code>null</code>.
	 * @return <code>True</code> if the given id belongs to a root node, <code>false</code> otherwise.
	 */
	private boolean isRootNode(String id) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(id);

		boolean isRoot = false;

		if (getService(IPeerModelLookupService.class).lkupPeerModelById(id) != null) {
			isRoot = true;
		} else {
			Map<String, IPeer> peers = Protocol.getLocator().getPeers();
			if (peers.containsKey(id)) {
				isRoot = true;
			}
		}

		return isRoot;
	}
}
