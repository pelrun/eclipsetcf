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
 * The service to update the properties of given locator node.
 */
public interface ILocatorModelUpdateService extends ILocatorModelService {

	/**
	 * Adds the given peer to the list of know peers.
	 *
	 * @param peer The peer to add. Must not be <code>null</code>.
	 * @return The new created or existing locator node.
	 */
	public ILocatorNode add(IPeer peer);

	/**
	 * Adds the given static peer to the list of know peers.
	 *
	 * @param peer The static peer to add. Must not be <code>null</code>.
	 * @param isStatic <code>true</code> if this node is a manually added static peer.
	 * @return The new created or existing locator node.
	 */
	public ILocatorNode add(IPeer peer, boolean isStatic);

	/**
	 * Removes the given peer from the list of known peers.
	 *
	 * @param peer The peer to remove. Must not be <code>null</code.
	 * @return The deleted locator node.
	 */
	public ILocatorNode remove(IPeer peer);

	/**
	 * Update an old peer with the given new one.
	 * @param oldPeer The old peer to update. Must not be <code>null</code.
	 * @param newPeer The new peer. Must not be <code>null</code.
	 * @return The updated locator node.
	 */
	public ILocatorNode update(IPeer oldPeer, IPeer newPeer);
}
