/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.internal.adapters;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.model.factory.Factory;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistableURIProvider;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModelProvider;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService;
import org.eclipse.tcf.te.tcf.locator.model.Model;

/**
 * Static peers adapter factory implementation.
 */
public class AdapterFactory implements IAdapterFactory {
	// The single instance adapter references
	private final IPersistableURIProvider peerModelPersistableURIProvider = new PeerPersistableURIProvider();

	private static final Class<?>[] CLASSES = new Class[] {
		IPersistableURIProvider.class, IPeerModel.class, IConnectable.class, ILocatorModel.class
	};

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof Map) {
			if (IPersistableURIProvider.class.equals(adapterType)) {
				Assert.isTrue(false);
			}
		}
		if (IConnectable.class.isAssignableFrom(adapterType)) {
			if (adaptableObject instanceof IConnectable) {
				return adaptableObject;
			}
		}
		if (ILocatorModel.class.isAssignableFrom(adapterType)) {
			if (adaptableObject instanceof IPeerModel) {
				return ((IPeerModel)adaptableObject).getModel();
			}
		}
		if (adaptableObject instanceof IPeerModel || adaptableObject instanceof IPeer || adaptableObject instanceof IPeerModelProvider) {
			if (IPersistableURIProvider.class.equals(adapterType)) {
				return peerModelPersistableURIProvider;
			}
			if (IPeerModel.class.equals(adapterType)) {
				if (adaptableObject instanceof IPeer) {
					final AtomicReference<IPeerModel> node = new AtomicReference<IPeerModel>();
					final IPeer peer = (IPeer)adaptableObject;

					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							String id = peer.getID();
							ILocatorModel model = Model.getModel();
							Assert.isNotNull(model);
							IPeerModel candidate = model.getService(ILocatorModelLookupService.class).lkupPeerModelById(id);
							if (candidate != null) node.set(candidate);
							else {
								candidate = Factory.getInstance().newInstance(IPeerModel.class, new Object[] { model, peer });
								if (candidate != null) node.set(candidate);
							}
						}
					};

					if (Protocol.isDispatchThread()) runnable.run();
					else Protocol.invokeAndWait(runnable);

					return node.get();
				}
				else if (adaptableObject instanceof IPeerModel) {
					return adaptableObject;
				}
				else if (adaptableObject instanceof IPeerModelProvider) {
					final AtomicReference<IPeerModel> node = new AtomicReference<IPeerModel>();
					final IPeerModelProvider provider = (IPeerModelProvider)adaptableObject;

					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							node.set(provider.getPeerModel());
						}
					};

					if (Protocol.isDispatchThread()) runnable.run();
					else Protocol.invokeAndWait(runnable);

					return node.get();
				}
			}
			if (IStepContext.class.equals(adapterType)) {
				if (adaptableObject instanceof IPeerModel) {
					return new PeerModelStepContext((IPeerModel)adaptableObject);
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
	 */
	@Override
	public Class[] getAdapterList() {
		return CLASSES;
	}

}
