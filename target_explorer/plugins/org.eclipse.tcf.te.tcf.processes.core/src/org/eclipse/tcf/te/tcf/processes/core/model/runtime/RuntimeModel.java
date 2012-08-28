/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.runtime;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.model.ContainerModelNode;
import org.eclipse.tcf.te.runtime.model.contexts.AsyncRefreshableCtxAdapter;
import org.eclipse.tcf.te.runtime.model.factory.Factory;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx.QueryState;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx.QueryType;
import org.eclipse.tcf.te.runtime.model.interfaces.factory.IFactory;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelChannelService;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelLookupService;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelRefreshService;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelService;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelUpdateService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.core.model.runtime.services.RuntimeModelChannelService;
import org.eclipse.tcf.te.tcf.processes.core.model.runtime.services.RuntimeModelLookupService;
import org.eclipse.tcf.te.tcf.processes.core.model.runtime.services.RuntimeModelRefreshService;
import org.eclipse.tcf.te.tcf.processes.core.model.runtime.services.RuntimeModelUpdateService;


/**
 * Model implementation dealing with Processes at runtime.
 */
public final class RuntimeModel extends ContainerModelNode implements IRuntimeModel {
	// Flag to mark the model disposed
	private boolean disposed;

	// Reference to the model node factory
	private IFactory factory = null;

	// Reference to the associated peer model
	private final IPeerModel peerModel;

	// Reference to the refresh service
	private final IModelRefreshService refreshService = new RuntimeModelRefreshService(this);
	// Reference to the lookup service
	private final IModelLookupService lookupService = new RuntimeModelLookupService(this);
	// Reference to the update service
	private final IModelUpdateService updateService = new RuntimeModelUpdateService(this);
	// Reference to the channel service
	private final IModelChannelService channelService = new RuntimeModelChannelService(this);

	// The runtime model needs asynchronous refreshes
	private final IAsyncRefreshableCtx refreshableCtxAdapter = new AsyncRefreshableCtxAdapter();

	/**
	 * Constructor.
	 *
	 * @param peerModel The peerModel to associated. Must not be <code>null</code>.
	 */
	public RuntimeModel(IPeerModel peerModel) {
		super();

		disposed = false;
		setChangeEventsEnabled(true);
		suppressEventsOnNullParent = false;

		Assert.isNotNull(peerModel);
		this.peerModel = peerModel;

		// No initial context query required
		refreshableCtxAdapter.setQueryState(QueryType.CONTEXT, QueryState.DONE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.nodes.PropertiesContainer#checkThreadAccess()
	 */
	@Override
	protected boolean checkThreadAccess() {
		return Protocol.isDispatchThread();
	}

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.core.model.interfaces.IModel#dispose()
	 */
	@Override
	public void dispose() {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		disposed = true;

		// Close the active channel (if any)
		channelService.closeChannel();
	}

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.core.model.interfaces.IModel#isDisposed()
	 */
	@Override
	public boolean isDisposed() {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		return disposed;
	}

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.core.model.interfaces.IModel#getService(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <V extends IModelService> V getService(Class<V> serviceInterface) {
		Assert.isNotNull(serviceInterface);
		return (V)getAdapter(serviceInterface);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (IModelRefreshService.class.equals(adapter)) {
			return refreshService;
		}
		if (IModelLookupService.class.equals(adapter)) {
			return lookupService;
		}
		if (IModelUpdateService.class.equals(adapter)) {
			return updateService;
		}
		if (IModelChannelService.class.equals(adapter)) {
			return channelService;
		}
		if (IAsyncRefreshableCtx.class.equals(adapter)) {
			return refreshableCtxAdapter;
		}

		return super.getAdapter(adapter);
	}

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.core.model.interfaces.IModel#setFactory(com.windriver.te.tcf.core.model.interfaces.IModelNodeFactory)
	 */
	@Override
	public void setFactory(IFactory factory) {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		this.factory = factory;
	}

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.core.model.interfaces.IModel#getFactory()
	 */
	@Override
	public IFactory getFactory() {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		return factory != null ? factory : Factory.getInstance();
	}

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.core.gdbremote.model.interfaces.IRuntimeModel#getPeerModel()
	 */
	@Override
	public IPeerModel getPeerModel() {
		Assert.isTrue(checkThreadAccess(), "Illegal Thread Access"); //$NON-NLS-1$
		return peerModel;
	}
}
