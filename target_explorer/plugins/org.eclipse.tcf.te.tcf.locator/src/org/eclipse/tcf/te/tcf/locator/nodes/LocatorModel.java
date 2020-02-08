/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Michael Jahn       - [452466] Running multiple agents on localhost
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ILocator;
import org.eclipse.tcf.te.runtime.utils.net.IPAddressUtil;
import org.eclipse.tcf.te.tcf.core.interfaces.ITransportTypes;
import org.eclipse.tcf.te.tcf.locator.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.ITracing;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelUpdateService;
import org.eclipse.tcf.te.tcf.locator.listener.LocatorListener;
import org.eclipse.tcf.te.tcf.locator.services.LocatorModelLookupService;
import org.eclipse.tcf.te.tcf.locator.services.LocatorModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.services.LocatorModelUpdateService;


/**
 * Default locator model implementation.
 */
public class LocatorModel extends PlatformObject implements ILocatorModel {
	// The unique model id
	private final UUID uniqueId = UUID.randomUUID();
	// Flag to mark the model disposed
	private boolean disposed;

	// The list of known locator nodes
	/* default */ final Map<String, ILocatorNode> locatorNodes = new HashMap<String, ILocatorNode>();

	// The list of registered model listeners
	private final List<ILocatorModelListener> modelListener = new ArrayList<ILocatorModelListener>();

	// Reference to the model locator listener
	private ILocator.LocatorListener locatorListener = null;

	// Reference to the refresh service
	private final ILocatorModelRefreshService refreshService = new LocatorModelRefreshService(this);
	// Reference to the lookup service
	private final ILocatorModelLookupService lookupService = new LocatorModelLookupService(this);
	// Reference to the update service
	private final ILocatorModelUpdateService updateService = new LocatorModelUpdateService(this);
	/**
	 * Constructor.
	 */
	public LocatorModel() {
		super();
		disposed = false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#addListener(org.eclipse.tcf.te.tcf.locator.core.interfaces.IModelListener)
	 */
	@Override
	public void addListener(ILocatorModelListener listener) {
		Assert.isNotNull(listener);

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("LocatorModel.addListener( " + listener + " )", ITracing.ID_TRACE_LOCATOR_MODEL, this); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (!modelListener.contains(listener)) modelListener.add(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#removeListener(org.eclipse.tcf.te.tcf.locator.core.interfaces.IModelListener)
	 */
	@Override
	public void removeListener(ILocatorModelListener listener) {
		Assert.isNotNull(listener);

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("LocatorModel.removeListener( " + listener + " )", ITracing.ID_TRACE_LOCATOR_MODEL, this); //$NON-NLS-1$ //$NON-NLS-2$
		}

		modelListener.remove(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel#getListener()
	 */
	@Override
	public ILocatorModelListener[] getListener() {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		return modelListener.toArray(new ILocatorModelListener[modelListener.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#dispose()
	 */
	@Override
	public void dispose() {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("LocatorModel.dispose()", ITracing.ID_TRACE_LOCATOR_MODEL, this); //$NON-NLS-1$
		}

		// If already disposed, we are done immediately
		if (disposed) return;

		disposed = true;

		if (locatorListener != null) {
			Protocol.getLocator().removeListener(locatorListener);
			locatorListener = null;
		}
		final ILocatorModelListener[] listeners = getListener();
		if (listeners.length > 0) {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					for (ILocatorModelListener listener : listeners) {
						listener.modelDisposed(LocatorModel.this);
					}
				}
			});
		}
		modelListener.clear();

		locatorNodes.clear();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#isDisposed()
	 */
	@Override
	public boolean isDisposed() {
		return disposed;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel#getLocatorNodes()
	 */
	@Override
	public ILocatorNode[] getLocatorNodes() {
	    return locatorNodes.values().toArray(new ILocatorNode[locatorNodes.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel#getPeers()
	 */
	@Override
	public IPeer[] getPeers() {
		List<IPeer> peers = new ArrayList<IPeer>();
		for (ILocatorNode locatorNode : locatorNodes.values()) {
	        peers.add(locatorNode.getPeer());
        }

		return peers.toArray(new IPeer[peers.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter.isAssignableFrom(ILocator.LocatorListener.class)) {
			return locatorListener;
		}
		if (adapter.isAssignableFrom(ILocatorModelRefreshService.class)) {
			return refreshService;
		}
		if (adapter.isAssignableFrom(ILocatorModelLookupService.class)) {
			return lookupService;
		}
		if (adapter.isAssignableFrom(ILocatorModelUpdateService.class)) {
			return updateService;
		}
		if (adapter.isAssignableFrom(Map.class)) {
			return locatorNodes;
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
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel#getService(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <V extends ILocatorModelService> V getService(Class<V> serviceInterface) {
		Assert.isNotNull(serviceInterface);
		return (V)getAdapter(serviceInterface);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel#validatePeer(org.eclipse.tcf.protocol.IPeer)
	 */
	@Override
	public IPeer validatePeer(IPeer peer) {
		Assert.isNotNull(peer);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// Skip validation if the transport type is not TCP or SSL
		String transport = peer.getTransportName();
		if (transport == null || !ITransportTypes.TRANSPORT_TYPE_TCP.equals(transport) && !ITransportTypes.TRANSPORT_TYPE_SSL.equals(transport)) {
			return peer;
		}

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("LocatorModel.validatePeer( " + peer.getID() + " )", ITracing.ID_TRACE_LOCATOR_MODEL, this); //$NON-NLS-1$ //$NON-NLS-2$
		}

		IPeer result = peer;

		// Get the loopback address
		String loopback = IPAddressUtil.getInstance().getIPv4LoopbackAddress();
		// Get the canonical address
		String canonical = IPAddressUtil.getInstance().getIPv4CanonicalAddress();
		// Get the peer IP and port
		String peerIP = peer.getAttributes().get(IPeer.ATTR_IP_HOST);
		String peerPort = peer.getAttributes().get(IPeer.ATTR_IP_PORT);

		// If the new peer IP is not a local host address, we are done checking
		if (!IPAddressUtil.getInstance().isLocalHost(peerIP)) return result;

		// The list of peers to remove from the model
		final List<String> toRemove = new ArrayList<String>();

		// Loop the discovered peers and find previous local host nodes
		for (Entry<String, ILocatorNode> entry : locatorNodes.entrySet()) {
			// Get the IP address from peers with transport type TCP or SSL
			IPeer candidate = entry.getValue().getPeer();
			if (ITransportTypes.TRANSPORT_TYPE_TCP.equals(candidate.getTransportName()) || ITransportTypes.TRANSPORT_TYPE_SSL.equals(candidate.getTransportName())) {
				String ip = candidate.getAttributes().get(IPeer.ATTR_IP_HOST);
				Assert.isNotNull(ip);
				String port = candidate.getAttributes().get(IPeer.ATTR_IP_PORT);
				Assert.isNotNull(port);

				// If the IP is for localhost, we have to do additional checking
				if (IPAddressUtil.getInstance().isLocalHost(ip) && port.equals(peerPort)) {
					// If the IP of the peer already in the model is the loopback address,
					// ignore all other.
					if (ip.equals(loopback)) { result = null; break; }

					// If the IP of the new peer IP is the loopback address, remove this peer
					if (peerIP.equals(loopback)) { toRemove.add(entry.getKey()); continue; }

					// None of the IP's are matching the loopback address, keep the one which is the canonical address
					if (ip.equals(canonical)) { result = null; break; }
					if (peerIP.equals(canonical)) { toRemove.add(entry.getKey()); continue; }

					// None of the IP's are matching the loopback nor the canonical address,
					// keep the one already in the model
					result = null; break;
				}
			}
		}

		// Remove any node identified to get removed. Do it via the update
		// service to make sure the notifications are send out.
		for (String candidate : toRemove) {
			if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
				CoreBundleActivator.getTraceHandler().trace("LocatorModel.validatePeer( " + peer.getID() + " ): Remove old peer with id " + candidate, //$NON-NLS-1$ //$NON-NLS-2$
															ITracing.ID_TRACE_LOCATOR_MODEL, this);
			}

			ILocatorNode node = locatorNodes.get(candidate);
			if (node != null) getService(ILocatorModelUpdateService.class).remove(node.getPeer());
		}

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("LocatorModel.validatePeer( " + peer.getID() + " ): result = " + (result != null ? result.getID() : "null"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
														ITracing.ID_TRACE_LOCATOR_MODEL, this);
		}

		// Return the result
	    return result;
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
	protected ILocator.LocatorListener doCreateLocatorListener(ILocatorModel model) {
		Assert.isNotNull(model);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		return new LocatorListener(model);
	}
}
