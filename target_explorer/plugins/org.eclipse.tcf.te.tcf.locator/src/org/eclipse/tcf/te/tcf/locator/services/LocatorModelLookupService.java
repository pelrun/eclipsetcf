/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.services;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService;


/**
 * Default locator model lookup service implementation.
 */
public class LocatorModelLookupService extends AbstractLocatorModelService implements ILocatorModelLookupService {

	/**
	 * Constructor.
	 *
	 * @param parentModel The parent locator model instance. Must not be <code>null</code>.
	 */
	public LocatorModelLookupService(ILocatorModel parentModel) {
		super(parentModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService#lkupPeerById(java.lang.String)
	 */
	@Override
	public IPeer lkupPeerById(String id) {
		Assert.isNotNull(id);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		IPeer node = null;
		for (IPeer peer : getLocatorModel().getPeers()) {
			if (id.equals(peer.getID())) {
				node = peer;
				break;
			}
		}

		return node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService#lkupPeerByAgentId(java.lang.String)
	 */
	@Override
	public IPeer[] lkupPeerByAgentId(String agentId) {
		Assert.isNotNull(agentId);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		List<IPeer> nodes = new ArrayList<IPeer>();
		for (IPeer peer : getLocatorModel().getPeers()) {
			if (agentId.equals(peer.getAgentID())) {
				nodes.add(peer);
			}
		}

		return nodes.toArray(new IPeer[nodes.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService#lkupPeerByName(java.lang.String)
	 */
	@Override
	public IPeer[] lkupPeerByName(String name) {
		Assert.isNotNull(name);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		List<IPeer> nodes = new ArrayList<IPeer>();
		for (IPeer peer : getLocatorModel().getPeers()) {
			if (name.equals(peer.getName())) {
				nodes.add(peer);
			}
		}

		return nodes.toArray(new IPeer[nodes.size()]);
	}
}
