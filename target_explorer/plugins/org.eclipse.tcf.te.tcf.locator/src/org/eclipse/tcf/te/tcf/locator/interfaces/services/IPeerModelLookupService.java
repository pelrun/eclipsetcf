/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces.services;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * The service to lookup/search in the parent locator model.
 */
public interface IPeerModelLookupService extends IPeerModelService {

	/**
	 * Lookup the peer model for the given peer id.
	 *
	 * @param id The peer id. Must not be <code>null</code>.
	 * @return The peer model instance, or <code>null</code> if the peer model cannot be found.
	 */
	public IPeerNode lkupPeerModelById(String id);

	/**
	 * Lookup the peer model for the given peer id from the peers child list of
	 * the peer identified by the given parent id.
	 *
	 * @param parentId The parent peer id. Must not be <code>null</code>.
	 * @param id The peer id. Must not be <code>null</code>.
	 * @return The peer model instance, or <code>null</code> if the peer model cannot be found.
	 */
	public IPeerNode lkupPeerModelById(String parentId, String id);

	/**
	 * Lookup the matching peer model instances for the given agent id.
	 *
	 * @param agentId The agent id. Must not be <code>null</code>.
	 * @return The peer model instances, or an empty list if the given agent id could not be matched.
	 */
	public IPeerNode[] lkupPeerModelByAgentId(String agentId);

	/**
	 * Lookup the matching peer model instances for the given agent id.
	 *
	 * @param parentId The parent peer id. Must not be <code>null</code>.
	 * @param agentId The agent id. Must not be <code>null</code>.
	 * @return The peer model instances, or an empty list if the given agent id could not be matched.
	 */
	public IPeerNode[] lkupPeerModelByAgentId(String parentId, String agentId);

	/**
	 * Lookup matching peer model instances for the given name.
	 *
	 * @param name The name. Must not be <code>null</code>.
	 * @return The peer model instances, or an empty list if the given name could not be matched.
	 */
	public IPeerNode[] lkupPeerModelByName(String name);

	/**
	 * Lookup matching peer model instances which supports the listed local and remote services.
	 * <p>
	 * <b>Note:</b> This method must be called outside the TCF dispatch thread.
	 *
	 * @param expectedLocalServices The list of local service names to be supported, or <code>null</code>.
	 * @param expectedRemoteServices The list of remote service names to be supported, or <code>null</code>.
	 *
	 * @return The peer model instances, or an empty list if the listed services are not supported by any of the peers.
	 */
	public IPeerNode[] lkupPeerModelBySupportedServices(String[] expectedLocalServices, String[] expectedRemoteServices);

	/**
	 * Lookup the matching static peer model instances for the given peer model node.
	 *
	 * @param peerNode The peer model node. Must not be <code>null</code>.
	 * @return The matching peer model instances or an empty list.
	 */
	public IPeerNode[] lkupMatchingStaticPeerModels(IPeerNode peerNode);

	/**
	 * Lookup the matching static peer model instances for the given peer.
	 *
	 * @param peer The peer or <code>null</code>.
	 * @return The matching peer model instances or an empty list.
	 */
	public IPeerNode[] lkupMatchingStaticPeerModels(IPeer peer);
}
