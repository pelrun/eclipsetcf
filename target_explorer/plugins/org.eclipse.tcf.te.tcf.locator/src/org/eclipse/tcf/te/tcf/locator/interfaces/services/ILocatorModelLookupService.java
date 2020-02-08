/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces.services;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;

/**
 * The service to lookup/search in the parent locator model.
 */
public interface ILocatorModelLookupService extends ILocatorModelService {

	/**
	 * Lookup the locator model for the given peer id.
	 *
	 * @param id The peer id. Must not be <code>null</code>.
	 * @return The peer instance, or <code>null</code> if the peer cannot be found.
	 */
	public IPeer lkupPeerById(String id);

	/**
	 * Lookup the matching peer instances for the given agent id.
	 *
	 * @param agentId The agent id. Must not be <code>null</code>.
	 * @return The peer instances, or an empty list if the given agent id could not be matched.
	 */
	public IPeer[] lkupPeerByAgentId(String agentId);

	/**
	 * Lookup matching peer instances for the given name.
	 *
	 * @param name The name. Must not be <code>null</code>.
	 * @return The peer instances, or an empty list if the given name could not be matched.
	 */
	public IPeer[] lkupPeerByName(String name);

	/**
	 * Lookup the matching locator node for the given id.
	 *
	 * @param parent The parent locator node or <code>null</code>.
	 * @param id The id. Must not be <code>null</code>.
	 * @return The locator node instances, or an empty list if the given id could not be matched.
	 */
	public ILocatorNode[] lkupLocatorNodeById(ILocatorNode parent, String id);

	/**
	 * Lookup the matching locator node for the given agent id.
	 *
	 * @param parent The parent locator node or <code>null</code>.
	 * @param agentId The agent id. Must not be <code>null</code>.
	 * @return The locator node instances, or an empty list if the given agent id could not be matched.
	 */
	public ILocatorNode[] lkupLocatorNodeByAgentId(ILocatorNode parent, String agentId);

	/**
	 * Lookup the matching locator node for the given peer.
	 *
	 * @param peer The peer.
	 * @return The locator node for the given peer or <code>null</code>.
	 */
	public ILocatorNode lkupLocatorNode(IPeer peer);
}
