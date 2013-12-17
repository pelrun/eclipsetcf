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
}
