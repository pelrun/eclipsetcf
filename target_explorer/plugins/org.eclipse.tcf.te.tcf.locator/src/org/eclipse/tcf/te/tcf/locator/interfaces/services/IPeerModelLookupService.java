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

import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * The service to lookup/search in the parent peer model.
 */
public interface IPeerModelLookupService extends IPeerModelService {

	/**
	 * Lookup the peer model for the given peer id.
	 *
	 * @param id The peer id. Must not be <code>null</code>.
	 * @return The peer node instance, or <code>null</code> if the peer node cannot be found.
	 */
	public IPeerNode lkupPeerModelById(String id);

	/**
	 * Lookup the matching peer node instances for the given agent id.
	 *
	 * @param agentId The agent id. Must not be <code>null</code>.
	 * @return The peer node instances, or an empty list if the given agent id could not be matched.
	 */
	public IPeerNode[] lkupPeerModelByAgentId(String agentId);

	/**
	 * Lookup matching peer node instances for the given name.
	 *
	 * @param name The name. Must not be <code>null</code>.
	 * @return The peer node instances, or an empty list if the given name could not be matched.
	 */
	public IPeerNode[] lkupPeerModelByName(String name);

	/**
	 * Lookup matching peer node instances which supports the listed local and remote services.
	 * <p>
	 * <b>Note:</b> This method must be called outside the TCF dispatch thread.
	 *
	 * @param expectedLocalServices The list of local service names to be supported, or <code>null</code>.
	 * @param expectedRemoteServices The list of remote service names to be supported, or <code>null</code>.
	 *
	 * @return The peer node instances, or an empty list if the listed services are not supported by any of the peers node.
	 */
	public IPeerNode[] lkupPeerModelBySupportedServices(String[] expectedLocalServices, String[] expectedRemoteServices);
}
