/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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
	 * Property: Peer ID of selected real target.
	 */
	public static final String PROPERTY_PEER_ID = "PeerId"; //$NON-NLS-1$

	/**
	 * Property: The peer valid state. This is not a property itself, just used to fire change events on valid state change.
	 */
	public static final String PROPERTY_IS_VALID = "isValid"; //$NON-NLS-1$
}
