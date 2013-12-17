/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.services;

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelUpdateService;


/**
 * Default peer model update service implementation.
 */
public class PeerModelUpdateService extends AbstractPeerModelService implements IPeerModelUpdateService {

	/**
	 * Constructor.
	 *
	 * @param parentModel The parent peer model instance. Must not be <code>null</code>.
	 */
	public PeerModelUpdateService(IPeerModel parentModel) {
		super(parentModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.services.ILocatorModelUpdateService#add(org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.IPeerModel)
	 */
	@Override
	public void add(final IPeerNode peerNode) {
		Assert.isNotNull(peerNode);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		Map<String, IPeerNode> peerNodes = (Map<String, IPeerNode>)getPeerModel().getAdapter(Map.class);
		Assert.isNotNull(peerNodes);
		peerNodes.put(peerNode.getPeerId(), peerNode);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.services.ILocatorModelUpdateService#remove(org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.IPeerModel)
	 */
	@Override
	public void remove(final IPeerNode peerNode) {
		Assert.isNotNull(peerNode);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		Map<String, IPeerNode> peerNodes = (Map<String, IPeerNode>)getPeerModel().getAdapter(Map.class);
		Assert.isNotNull(peerNodes);
		peerNodes.remove(peerNode.getPeerId());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.services.ILocatorModelUpdateService#updatePeerServices(org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.IPeerModel, java.util.Collection, java.util.Collection)
	 */
	@Override
	public void updatePeerServices(IPeerNode peerNode, Collection<String> localServices, Collection<String> remoteServices) {
		Assert.isNotNull(peerNode);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		peerNode.setProperty(IPeerNodeProperties.PROP_LOCAL_SERVICES, localServices != null ? makeString(localServices) : null);
		peerNode.setProperty(IPeerNodeProperties.PROP_REMOTE_SERVICES, remoteServices != null ? makeString(remoteServices) : null);
	}

	/**
	 * Transform the given collection into a plain string.
	 *
	 * @param collection The collection. Must not be <code>null</code>.
	 * @return The plain string.
	 */
	protected String makeString(Collection<String> collection) {
		Assert.isNotNull(collection);

		if (collection.isEmpty()) return null;

		String buffer = collection.toString();
		buffer = buffer.replaceAll("\\[", "").replaceAll("\\]", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		return buffer.trim();
	}
}
