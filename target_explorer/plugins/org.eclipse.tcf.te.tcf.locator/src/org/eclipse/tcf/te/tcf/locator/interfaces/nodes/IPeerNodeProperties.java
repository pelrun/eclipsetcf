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
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;

/**
 * Default set of peer node properties.
 */
public interface IPeerNodeProperties {

	/**
	 * Property: The peer node connect state.
	 */
	public static final String PROP_CONNECT_STATE = "connectState"; //$NON-NLS-1$

	/**
	 * Property: The peer instance. Object stored here must be
	 *           castable to {@link IPeer}.
	 */
	public static final String PROP_INSTANCE = "instance"; //$NON-NLS-1$

	/**
	 * Property: The list of known local service names.
	 */
	public static final String PROP_LOCAL_SERVICES = "services.local"; //$NON-NLS-1$

	/**
	 * Property: The list of known remote service names.
	 */
	public static final String PROP_REMOTE_SERVICES = "services.remote"; //$NON-NLS-1$

	/**
	 * Property: Peer ID of selected real target.
	 */
	public static final String PROP_PEER_ID = "PeerId"; //$NON-NLS-1$

	/**
	 * Property: Ping intervall.
	 */
	public static String PROP_PING_INTERVAL = "pingInterval"; //$NON-NLS-1$

	/**
	 * Property: Ping timeout.
	 */
	public static String PROP_PING_TIMEOUT = "pingTimeout"; //$NON-NLS-1$

	/**
	 * Property: The peer valid state. This is not a property itself, just used to fire change events on valid state change.
	 */
	public static final String PROP_VALID = "valid"; //$NON-NLS-1$

	/**
	 * Property: The peer type.
	 */
	public static final String PROP_TYPE = "Type" + IPropertiesContainer.PERSISTENT_PROPERTY; //$NON-NLS-1$

	/**
	 * Property: List of TCF services the peer would have when it goes online (comma separated list).
	 */
	public static final String PROP_OFFLINE_SERVICES = "OfflineServices" + IPropertiesContainer.PERSISTENT_PROPERTY; //$NON-NLS-1$

	/**
	 * Property: The peer visible state.
	 */
	public static final String PROP_VISIBLE = "Visible" + IPropertiesContainer.PERSISTENT_PROPERTY; //$NON-NLS-1$

	/**
	 * Property: Launch simulator
	 */
	public static final String PROP_SIM_ENABLED = "SimulatorEnabled" + IPropertiesContainer.PERSISTENT_PROPERTY; //$NON-NLS-1$

	/**
	 * Property: Simulator properties
	 */
	public static final String PROP_SIM_PROPERTIES = "SimulatorProperties" + IPropertiesContainer.PERSISTENT_PROPERTY; //$NON-NLS-1$

	/**
	 * Property: Last selected simulator type
	 */
	public static final String PROP_SIM_TYPE = "SimulatorType" + IPropertiesContainer.PERSISTENT_PROPERTY; //$NON-NLS-1$

	/**
	 * Property: Auto-start the debugger after the agent launch.
	 */
	public static final String PROP_AUTO_START_DEBUGGER = "autoStartDebugger" + IPropertiesContainer.PERSISTENT_PROPERTY; //$NON-NLS-1$

	/**
	 * Property: Connect after the configuration has been created.
	 */
	public static final String PROP_AUTO_CONNECT = "autoConnect"; //$NON-NLS-1$
}
