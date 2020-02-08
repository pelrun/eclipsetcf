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

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.core.util.persistence.PeerDataHelper;
import org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelUpdateService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.locator.nodes.LocatorNode;


/**
 * Default locator model update service implementation.
 */
public class LocatorModelUpdateService extends AbstractLocatorModelService implements ILocatorModelUpdateService {

	/**
	 * Constructor.
	 *
	 * @param parentModel The parent locator model instance. Must not be <code>null</code>.
	 */
	public LocatorModelUpdateService(ILocatorModel parentModel) {
		super(parentModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelUpdateService#add(org.eclipse.tcf.protocol.IPeer)
	 */
	@Override
	public ILocatorNode add(IPeer peer) {
		return add(peer, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelUpdateService#add(org.eclipse.tcf.protocol.IPeer, boolean)
	 */
	@SuppressWarnings("unchecked")
    @Override
	public ILocatorNode add(final IPeer peer, boolean isStatic) {
		Assert.isNotNull(peer);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		String encProxies = peer.getAttributes().get(IPeerProperties.PROP_PROXIES);
		ILocatorNode locatorNode = null;

		if (encProxies == null || encProxies.trim().length() == 0) {
			Map<String, ILocatorNode> locatorNodes = (Map<String, ILocatorNode>)getLocatorModel().getAdapter(Map.class);
			Assert.isNotNull(locatorNodes);
			ILocatorModelLookupService lkup = ModelManager.getLocatorModel().getService(ILocatorModelLookupService.class);
			locatorNode = lkup.lkupLocatorNode(peer);
			if (locatorNode == null) {
				locatorNode = new LocatorNode(peer, isStatic);
				locatorNodes.put(peer.getID(), locatorNode);
			}
			else if (isStatic) {
				locatorNode.setProperty(ILocatorNode.PROPERTY_STATIC_INSTANCE, peer);
			}
		}
		else {
			IPeer[] proxies = PeerDataHelper.decodePeerList(encProxies);
			ILocatorNode parent = null;
			for (IPeer proxy : proxies) {
				ILocatorModelLookupService lkup = ModelManager.getLocatorModel().getService(ILocatorModelLookupService.class);
				ILocatorNode proxyNode = lkup.lkupLocatorNode(proxy);
				if (proxyNode == null) {
					proxyNode = new LocatorNode(proxy, true);
					if (parent == null) {
						Map<String, ILocatorNode> locatorNodes = (Map<String, ILocatorNode>)getLocatorModel().getAdapter(Map.class);
						Assert.isNotNull(locatorNodes);
						locatorNodes.put(proxy.getID(), proxyNode);

					}
					else {
						parent.add(proxyNode);
					}
					parent = proxyNode;
				}
				else {
					parent = proxyNode;
				}
            }
			ILocatorModelLookupService lkup = ModelManager.getLocatorModel().getService(ILocatorModelLookupService.class);
			locatorNode = lkup.lkupLocatorNode(peer);
			if (locatorNode == null) {
				locatorNode = new LocatorNode(peer, true);
				parent.add(locatorNode);
			}
		}

		ILocatorModelListener[] listeners = getLocatorModel().getListener();
		for (ILocatorModelListener listener : listeners) {
			listener.modelChanged(getLocatorModel(), locatorNode, true);
		}

		return locatorNode;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelUpdateService#remove(org.eclipse.tcf.protocol.IPeer)
	 */
	@SuppressWarnings("unchecked")
    @Override
	public ILocatorNode remove(final IPeer peer) {
		Assert.isNotNull(peer);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		ILocatorModelLookupService lkup = ModelManager.getLocatorModel().getService(ILocatorModelLookupService.class);
		ILocatorNode locatorNode = lkup.lkupLocatorNode(peer);

		if (locatorNode != null) {
			ILocatorNode parent = locatorNode.getParent(ILocatorNode.class);

			if (parent == null) {
				Map<String, ILocatorNode> locatorNodes = (Map<String, ILocatorNode>)getLocatorModel().getAdapter(Map.class);
				Assert.isNotNull(locatorNodes);
				locatorNode = locatorNodes.remove(peer.getID());
			}
			else {
				parent.remove(locatorNode, true);
			}
		}

		ILocatorModelListener[] listeners = getLocatorModel().getListener();
		for (ILocatorModelListener listener : listeners) {
			listener.modelChanged(getLocatorModel(), locatorNode, false);
		}

		return locatorNode;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelUpdateService#update(org.eclipse.tcf.protocol.IPeer, org.eclipse.tcf.protocol.IPeer)
	 */
	@SuppressWarnings("unchecked")
    @Override
	public ILocatorNode update(final IPeer oldPeer, final IPeer newPeer) {
		Assert.isNotNull(oldPeer);
		Assert.isNotNull(newPeer);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		Map<String, ILocatorNode> locatorNodes = (Map<String, ILocatorNode>)getLocatorModel().getAdapter(Map.class);
		Assert.isNotNull(locatorNodes);
		final ILocatorNode oldLocatorNode = locatorNodes.remove(oldPeer.getID());
		final ILocatorNode newLocatorNode = new LocatorNode(newPeer);
		locatorNodes.put(newPeer.getID(), newLocatorNode);

		ILocatorModelListener[] listeners = getLocatorModel().getListener();
		for (ILocatorModelListener listener : listeners) {
			if (!oldPeer.getID().equals(newPeer.getID())) {
				listener.modelChanged(getLocatorModel(), oldLocatorNode, false);
				listener.modelChanged(getLocatorModel(), newLocatorNode, true);
			}
			else {
				listener.modelChanged(getLocatorModel(), newLocatorNode, false);
			}
		}

		return newLocatorNode;
	}
}
