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
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ILocator;
import org.eclipse.tcf.te.tcf.locator.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.ITracing;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
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

	// The list of known peers
	/* default */ final Map<String, IPeer> peers = new HashMap<String, IPeer>();
	// The list of "proxied" peers per proxy peer id
	/* default */ final Map<String, List<IPeer>> peerChildren = new HashMap<String, List<IPeer>>();

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
	public void removeListener(ILocatorModelListener listener) {
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
			CoreBundleActivator.getTraceHandler().trace("PeerModel.dispose()", ITracing.ID_TRACE_LOCATOR_MODEL, this); //$NON-NLS-1$
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
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel#getPeers()
	 */
	@Override
	public IPeer[] getPeers() {
		return peers.values().toArray(new IPeer[peers.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel#getChildren(java.lang.String)
	 */
	@Override
    public List<IPeer> getChildren(final String parentPeerID) {
		Assert.isNotNull(parentPeerID);

		final AtomicReference<List<IPeer>> result = new AtomicReference<List<IPeer>>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				List<IPeer> children = peerChildren.get(parentPeerID);
				if (children == null) children = Collections.emptyList();
				result.set(children);
			}
		};

		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeAndWait(runnable);

		return Collections.unmodifiableList(result.get());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel#setChildren(java.lang.String, java.util.List)
	 */
	@Override
    public void setChildren(String parentPeerID, List<IPeer> children) {
		Assert.isNotNull(parentPeerID);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (children == null || children.size() == 0) {
			peerChildren.remove(parentPeerID);
		} else {
			peerChildren.put(parentPeerID, new ArrayList<IPeer>(children));
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
		if (obj instanceof LocatorModel) {
			return uniqueId.equals(((LocatorModel)obj).uniqueId);
		}
		return super.equals(obj);
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
