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
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.core.util.persistence.PeerDataHelper;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;
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

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService#lkupLocatorNodeById(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode, java.lang.String)
	 */
	@Override
	public ILocatorNode[] lkupLocatorNodeById(ILocatorNode parent, String id) {
		Assert.isNotNull(id);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		List<ILocatorNode> locatorNodes;
		if (parent != null) {
			locatorNodes = parent.getChildren(ILocatorNode.class);
		}
		else {
			locatorNodes = Arrays.asList(getLocatorModel().getLocatorNodes());
		}

		List<ILocatorNode> nodes = new ArrayList<ILocatorNode>();
		for (ILocatorNode node : locatorNodes) {
			if (id.equals(node.getPeer().getID())) {
				nodes.add(node);
			}
		}

		return nodes.toArray(new ILocatorNode[nodes.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService#lkupLocatorNodeByAgentId(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode, java.lang.String)
	 */
	@Override
	public ILocatorNode[] lkupLocatorNodeByAgentId(ILocatorNode parent, String agentId) {
		Assert.isNotNull(agentId);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		List<ILocatorNode> locatorNodes;
		if (parent != null) {
			locatorNodes = parent.getChildren(ILocatorNode.class);
		}
		else {
			locatorNodes = Arrays.asList(getLocatorModel().getLocatorNodes());
		}

		List<ILocatorNode> nodes = new ArrayList<ILocatorNode>();
		for (ILocatorNode node : locatorNodes) {
			String nodeAgentId = node.getPeer().getAgentID();
			if (nodeAgentId == null) {
				nodeAgentId = node.getStringProperty(IPeer.ATTR_AGENT_ID);
			}
			if (agentId.equals(nodeAgentId)) {
				nodes.add(node);
			}
		}

		return nodes.toArray(new ILocatorNode[nodes.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService#lkupLocatorNode(org.eclipse.tcf.protocol.IPeer)
	 */
	@Override
	public ILocatorNode lkupLocatorNode(IPeer peer) {
		Assert.isNotNull(peer);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		String encProxies = peer.getAttributes().get(IPeerProperties.PROP_PROXIES);
		IPeer[] proxies = PeerDataHelper.decodePeerList(encProxies);

		ILocatorNode parent = null;

		for (IPeer proxy : proxies) {
			String agentId = proxy.getAgentID();
			String id = proxy.getID();
			ILocatorNode[] nodes = null;
			if (agentId != null) {
		        nodes = lkupLocatorNodeByAgentId(parent, agentId);
			}
			if ((nodes == null || nodes.length == 0 ) && id != null) {
		        nodes = lkupLocatorNodeById(parent, id);
			}

			if (nodes != null && nodes.length > 0) {
				parent = nodes[0];
			}
			else {
				parent = null;
				break;
			}
        }

		String agentId = peer.getAgentID();
		String id = peer.getID();
		ILocatorNode[] nodes = null;
		if (agentId != null) {
	        nodes = lkupLocatorNodeByAgentId(parent, agentId);
		}
		if ((nodes == null || nodes.length == 0 ) && id != null) {
	        nodes = lkupLocatorNodeById(parent, id);
		}
		if (nodes != null && nodes.length > 0) {
			return nodes[0];
		}

	    return null;
	}
}
