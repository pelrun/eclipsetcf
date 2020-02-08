/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
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
 * The service to query asynchronous properties of peer nodes.
 */
public interface IPeerModelQueryService extends IPeerModelService {

	/**
	 * Returns the list of available local services.
	 * <b>Note:</b> This method must be called outside the TCF dispatch thread.
	 *
	 * @param node The peer node. Must not be <code>null</code>.
	 * @return List of local services or null if target is not connected.
	 */
	public String[] queryLocalServices(IPeerNode node);

	/**
	 * Returns the list of available remote services.
	 * <b>Note:</b> This method must be called outside the TCF dispatch thread.
	 *
	 * @param node The peer node. Must not be <code>null</code>.
	 * @return List of remote services or null if target is not connected.
	 */
	public String[] queryRemoteServices(IPeerNode node);

	/**
	 * Returns the list of offline services.
	 * <b>Note:</b> This method must be called outside the TCF dispatch thread.
	 *
	 * @param node The peer node. Must not be <code>null</code>.
	 * @return List of offline services or empty list.
	 */
	public String[] queryOfflineServices(IPeerNode node);

	/**
	 * Check if the given local services are available on the given target.
	 * @param node The peer node. Must not be <code>null</code>.
	 * @param service List of services.
	 * @return <code>true</code> if all services are available.
	 */
	public boolean hasLocalService(IPeerNode node, String... service);

	/**
	 * Check if the given remote services are available on the given target.
	 * @param node The peer node. Must not be <code>null</code>.
	 * @param service List of services.
	 * @return <code>true</code> if all services are available.
	 */
	public boolean hasRemoteService(IPeerNode node, String... service);

	/**
	 * Check if the given offline services are available on the given target.
	 * @param node The peer node. Must not be <code>null</code>.
	 * @param service List of services.
	 * @return <code>true</code> if all services are available.
	 */
	public boolean hasOfflineService(IPeerNode node, String... service);
}
