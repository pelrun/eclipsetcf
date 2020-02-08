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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelQueryService;


/**
 * Default peer model lookup service implementation.
 */
public class PeerModelLookupService extends AbstractPeerModelService implements IPeerModelLookupService {

	/**
	 * Constructor.
	 *
	 * @param parentModel The parent peer model instance. Must not be <code>null</code>.
	 */
	public PeerModelLookupService(IPeerModel parentModel) {
		super(parentModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.services.ILocatorModelLookupService#lkupPeerModelById(java.lang.String)
	 */
	@Override
	public IPeerNode lkupPeerModelById(String id) {
		Assert.isNotNull(id);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		IPeerNode node = null;
		for (IPeerNode candidate : getPeerModel().getPeerNodes()) {
			IPeer peer = candidate.getPeer();
			if (id.equals(peer.getID())) {
				node = candidate;
				break;
			} else if (peer.getAttributes().get("remote.id.transient") != null //$NON-NLS-1$
							&& peer.getAttributes().get("remote.id.transient").equals(id)) { //$NON-NLS-1$
				node = candidate;
				break;
			}
		}

		return node;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService#lkupPeerModelByAgentId(java.lang.String)
	 */
	@Override
	public IPeerNode[] lkupPeerModelByAgentId(String agentId) {
		Assert.isNotNull(agentId);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		List<IPeerNode> nodes = new ArrayList<IPeerNode>();
		for (IPeerNode candidate : getPeerModel().getPeerNodes()) {
			IPeer peer = candidate.getPeer();
			if (agentId.equals(peer.getAgentID())) {
				nodes.add(candidate);
			}
		}

		return nodes.toArray(new IPeerNode[nodes.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService#lkupPeerModelByName(java.lang.String)
	 */
	@Override
	public IPeerNode[] lkupPeerModelByName(String name) {
		Assert.isNotNull(name);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		List<IPeerNode> nodes = new ArrayList<IPeerNode>();
		for (IPeerNode candidate : getPeerModel().getPeerNodes()) {
			IPeer peer = candidate.getPeer();
			if (name.equals(peer.getName())) {
				nodes.add(candidate);
			}
		}

		return nodes.toArray(new IPeerNode[nodes.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService#lkupPeerModelBySupportedServices(java.lang.String[], java.lang.String[])
	 */
	@Override
	public IPeerNode[] lkupPeerModelBySupportedServices(String[] expectedLocalServices, String[] expectedRemoteServices) {
		Assert.isTrue(!Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		IPeerModel model = getPeerModel();
		IPeerModelQueryService queryService = model.getService(IPeerModelQueryService.class);

		List<IPeerNode> nodes = new ArrayList<IPeerNode>();
		for (IPeerNode candidate : model.getPeerNodes()) {
			if (queryService != null &&
							(expectedLocalServices == null || queryService.hasLocalService(candidate, expectedLocalServices)) &&
							(expectedRemoteServices == null || queryService.hasRemoteService(candidate, expectedRemoteServices))) {
				nodes.add(candidate);
			}
		}

		return nodes.toArray(new IPeerNode[nodes.size()]);
	}
}
