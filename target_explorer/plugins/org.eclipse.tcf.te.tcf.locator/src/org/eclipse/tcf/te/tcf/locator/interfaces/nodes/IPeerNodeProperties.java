/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces.nodes;

import org.eclipse.tcf.protocol.IPeer;

/**
 * Default set of peer node properties.
 */
public interface IPeerNodeProperties {

	/**
	 * Property: The peer node connect state.
	 */
	public static final String PROPERTY_CONNECT_STATE = "ConnectState"; //$NON-NLS-1$

	/**
	 * Property: The peer instance. Object stored here must be
	 *           castable to {@link IPeer}.
	 */
	public static final String PROPERTY_INSTANCE = "PeerInstance"; //$NON-NLS-1$

	/**
	 * Property: The list of known local service names.
	 */
	public static final String PROPERTY_LOCAL_SERVICES = "LocalServices"; //$NON-NLS-1$

	/**
	 * Property: The list of known remote service names.
	 */
	public static final String PROPERTY_REMOTE_SERVICES = "RemoteServices"; //$NON-NLS-1$

	/**
	 * Property: Peer ID.
	 */
	public static final String PROPERTY_PEER_ID = "PeerId"; //$NON-NLS-1$

	/**
	 * Property: The peer node valid state. This is not a property itself, just used to fire change events on valid state change.
	 */
	public static final String PROPERTY_IS_VALID = "isValid"; //$NON-NLS-1$

	/**
	 * Property: The peer node deleted state. Set to "true" if the peer node got deleted.
	 */
	public static final String PROPERTY_IS_DELETED = "isDeleted"; //$NON-NLS-1$

	/**
	 * Property: Error if peer node is not valid (String)
	 */
	public static final String PROPERTY_ERROR = "Error"; //$NON-NLS-1$

	/**
	 * Property: Container for warnings (Map<String,String)
	 */
	public static final String PROPERTY_WARNINGS = "Warnings"; //$NON-NLS-1$

	/**
	 * Property: Contains for warnings origins (Map<String,String)
	 */
	public static final String PROPERTY_WARNING_ORIGINS = "WarningOrigins"; //$NON-NLS-1$

	/**
	 * Property: Exit error if any simulator or other started process died during connect.
	 */
	public static final String PROP_EXIT_ERROR = "ExitError"; //$NON-NLS-1$
}
