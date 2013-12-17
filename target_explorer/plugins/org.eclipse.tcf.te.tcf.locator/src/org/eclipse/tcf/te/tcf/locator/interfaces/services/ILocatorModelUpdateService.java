/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
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
 * The service to update the properties of given locator node.
 */
public interface ILocatorModelUpdateService extends ILocatorModelService {

	/**
	 * Adds the given peer to the list of know peers.
	 *
	 * @param peer The peer to add. Must not be <code>null</code>.
	 */
	public void add(IPeer peer);

	/**
	 * Removes the given peer from the list of known peers.
	 *
	 * @param peer The peer to remove. Must not be <code>null</code.
	 */
	public void remove(IPeer peer);

	/**
	 * Update an old peer with the given new one.
	 * @param oldPeer The old peer to update. Must not be <code>null</code.
	 * @param newPeer The new peer. Must not be <code>null</code.
	 */
	public void update(IPeer oldPeer, IPeer newPeer);
}
