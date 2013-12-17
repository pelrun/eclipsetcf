/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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
	 * Query the list of available local services for the given peer node.
	 * <p>
	 * <b>Note:</b> The result of the query is cached within the given peer
	 * node and subsequent calls will return immediately with the cached value.
	 * <p>
	 * <b>Note:</b> This method must be called outside the TCF dispatch thread.
	 *
	 * @param node The peer node. Must not be <code>null</code>.
	 * @param done The client callback. Must not be <code>null</code>.
	 */
	public String queryLocalServices(IPeerNode node);

	/**
	 * Query the list of available remote services for the given peer node.
	 * <p>
	 * <b>Note:</b> The result of the query is cached within the given peer
	 * node and subsequent calls will return immediately with the cached value.
	 * <p>
	 * <b>Note:</b> This method must be called outside the TCF dispatch thread.
	 *
	 * @param node The peer node. Must not be <code>null</code>.
	 * @param done The client callback. Must not be <code>null</code>.
	 */
	public String queryRemoteServices(IPeerNode node);

	/**
	 * Client call back interface for queryServicesAsync(...).
	 */
	public interface DoneQueryServices {
		/**
		 * Called when the services query completed.
		 *
		 * @param error The error description if operation failed, <code>null</code> if succeeded.
		 */
		void doneQueryServices(Throwable error);
	}

	/**
	 * Asynchronously query the services for the given peer node.
	 * <p>
	 * <b>Note:</b> This method must be called from within the TCF dispatch thread.
	 *
	 * @param node The peer node. Must not be <code>null</code>.
	 * @param done The client callback. Must not be <code>null</code>.
	 */
	public void queryServicesAsync(IPeerNode node, DoneQueryServices done);

}
