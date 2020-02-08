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

import java.util.Collection;

import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * The service to update the properties of given peer node.
 */
public interface IPeerModelUpdateService extends IPeerModelService {

	/**
	 * Adds the given peer node to the list of know peer nodes.
	 *
	 * @param peer The peer node to add. Must not be <code>null</code>.
	 */
	public void add(IPeerNode peer);

	/**
	 * Removes the given peer node from the list of known peer nodes.
	 *
	 * @param peer The peer node to remove. Must not be <code>null</code.
	 */
	public void remove(IPeerNode peer);

	/**
	 * Update the service nodes of the given peer node with the new set of
	 * local and/or remote services.
	 *
	 * @param peerNode The peer node instance. Must not be <code>null</code>.
	 * @param localServices The list of local service names or <code>null</code>.
	 * @param remoteServices The list of remote service names or <code>null</code>.
	 */
	public void updatePeerServices(IPeerNode peerNode, Collection<String> localServices, Collection<String> remoteServices);

}
