/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.ITracing;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelQueryService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelUpdateService;
import org.eclipse.tcf.te.tcf.locator.services.PeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.services.PeerModelQueryService;
import org.eclipse.tcf.te.tcf.locator.services.PeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.services.PeerModelUpdateService;


/**
 * Default peer model implementation.
 */
public class PeerModel extends PlatformObject implements IPeerModel {
	// The unique model id
	private final UUID uniqueId = UUID.randomUUID();
	// Flag to mark the model disposed
	private boolean disposed;

	// The list of known peer nodes
	/* default */ final Map<String, IPeerNode> peerNodes = new HashMap<String, IPeerNode>();

	// The list of registered model listeners
	private final List<IPeerModelListener> modelListener = new ArrayList<IPeerModelListener>();

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
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#addListener(org.eclipse.tcf.te.tcf.locator.core.interfaces.IModelListener)
	 */
	@Override
	public void addListener(IPeerModelListener listener) {
		Assert.isNotNull(listener);

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_PEER_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("PeerModel.addListener( " + listener + " )", ITracing.ID_TRACE_PEER_MODEL, this); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (!modelListener.contains(listener)) modelListener.add(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#removeListener(org.eclipse.tcf.te.tcf.locator.core.interfaces.IModelListener)
	 */
	@Override
	public void removeListener(IPeerModelListener listener) {
		Assert.isNotNull(listener);

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_PEER_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("PeerModel.removeListener( " + listener + " )", ITracing.ID_TRACE_PEER_MODEL, this); //$NON-NLS-1$ //$NON-NLS-2$
		}

		modelListener.remove(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel#getListener()
	 */
	@Override
	public IPeerModelListener[] getListener() {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		return modelListener.toArray(new IPeerModelListener[modelListener.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#dispose()
	 */
	@Override
	public void dispose() {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_PEER_MODEL)) {
			CoreBundleActivator.getTraceHandler().trace("PeerModel.dispose()", ITracing.ID_TRACE_PEER_MODEL, this); //$NON-NLS-1$
		}

		// If already disposed, we are done immediately
		if (disposed) return;

		disposed = true;

		final IPeerModelListener[] listeners = getListener();
		if (listeners.length > 0) {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					for (IPeerModelListener listener : listeners) {
						listener.modelDisposed(PeerModel.this);
					}
				}
			});
		}
		modelListener.clear();

		peerNodes.clear();
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
	public IPeerNode[] getPeerNodes() {
		// We explicitly allow to access the current set of peer nodes
		// from any thread. The method returns a snapshot of the current
		// set of peer nodes.
		List<IPeerNode> values = new ArrayList<IPeerNode>(peerNodes.values());
		return values.toArray(new IPeerNode[values.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
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
			return peerNodes;
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
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel#getService(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <V extends IPeerModelService> V getService(Class<V> serviceInterface) {
		Assert.isNotNull(serviceInterface);
		return (V)getAdapter(serviceInterface);
	}
}
