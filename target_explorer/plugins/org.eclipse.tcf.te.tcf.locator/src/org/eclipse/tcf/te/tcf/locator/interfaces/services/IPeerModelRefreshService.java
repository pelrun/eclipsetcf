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

import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * The service to refresh the parent locator model from remote.
 */
public interface IPeerModelRefreshService extends IPeerModelService {

	/**
	 * Refreshes the list of known peers from the local locator service
	 * and update the locator model.
	 *
	 * @param callback The callback to invoke once the refresh operation finished, or <code>null</code>.
	 */
	public void refresh(ICallback callback);

	/**
	 * Refreshes the list of static peers only and merge them with the peers
	 * already known to the locator model via the dynamic discovery.
	 */
	public void refreshStaticPeers();

	/**
	 * Refresh the agent ID's of the given or all reachable static peers.
	 *
	 * @param nodes The list of nodes to refresh or <code>null</code> to refresh all.
	 * @param callback The callback to invoke once the refresh operation finished, or <code>null</code>.
	 */
	public void refreshAgentIDs(IPeerNode[] nodes, ICallback callback);
}
