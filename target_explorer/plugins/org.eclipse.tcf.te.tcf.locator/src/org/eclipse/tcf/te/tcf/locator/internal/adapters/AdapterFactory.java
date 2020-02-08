/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
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
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;

/**
 * Static peers adapter factory implementation.
 */
public class AdapterFactory implements IAdapterFactory {
	// The single instance adapter references
	private final IPersistableURIProvider peerModelPersistableURIProvider = new PeerPersistableURIProvider();

	private static final Class<?>[] CLASSES = new Class[] {
		IPersistableURIProvider.class, IPeerNode.class, IConnectable.class, IPeerModel.class
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
		if (IPeerModel.class.isAssignableFrom(adapterType)) {
			if (adaptableObject instanceof IPeerNode) {
				return ((IPeerNode)adaptableObject).getModel();
			}
		}
		if (adaptableObject instanceof IPeerNode || adaptableObject instanceof IPeer || adaptableObject instanceof IPeerNodeProvider) {
			if (IPersistableURIProvider.class.equals(adapterType)) {
				return peerModelPersistableURIProvider;
			}
			if (IPeerNode.class.equals(adapterType)) {
				if (adaptableObject instanceof IPeer) {
					final AtomicReference<IPeerNode> node = new AtomicReference<IPeerNode>();
					final IPeer peer = (IPeer)adaptableObject;

					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							String id = peer.getID();
							IPeerModel model = ModelManager.getPeerModel();
							Assert.isNotNull(model);
							IPeerNode candidate = model.getService(IPeerModelLookupService.class).lkupPeerModelById(id);
							if (candidate != null) node.set(candidate);
							else {
								candidate = Factory.getInstance().newInstance(IPeerNode.class, new Object[] { model, peer });
								if (candidate != null) node.set(candidate);
							}
						}
					};

					if (Protocol.isDispatchThread()) runnable.run();
					else Protocol.invokeAndWait(runnable);

					return node.get();
				}
				else if (adaptableObject instanceof IPeerNode) {
					return adaptableObject;
				}
				else if (adaptableObject instanceof IPeerNodeProvider) {
					final AtomicReference<IPeerNode> node = new AtomicReference<IPeerNode>();
					final IPeerNodeProvider provider = (IPeerNodeProvider)adaptableObject;

					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							node.set(provider.getPeerNode());
						}
					};

					if (Protocol.isDispatchThread()) runnable.run();
					else Protocol.invokeAndWait(runnable);

					return node.get();
				}
			}
			if (IStepContext.class.equals(adapterType)) {
				if (adaptableObject instanceof IPeer) {
					return new PeerStepContext((IPeer)adaptableObject);
				}
				if (adaptableObject instanceof IPeerNode) {
					return new PeerNodeStepContext((IPeerNode)adaptableObject);
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
