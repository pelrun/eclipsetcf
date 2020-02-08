/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.interfaces;

import org.eclipse.tcf.te.core.nodes.interfaces.wire.IWireTypeNetwork;
import org.eclipse.tcf.te.tcf.core.util.persistence.PeerDataHelper;

/**
 * Custom peer property IDs.
 */
public interface IPeerProperties {

	/**
	 * Property: The list of proxies to use to connect to the target.
	 *           The value of this property is of type <code>String</code> and
	 *           must be decoded using {@link PeerDataHelper#decodePeerList(String)}.
	 */
	public static final String PROP_PROXIES = "Proxies"; //$NON-NLS-1$

	/**
	 * Property: The version of the connection.
	 */
	public static final String PROP_VERSION = "Version";  //$NON-NLS-1$

	/**
	 * Property: <code>true</code> if this connection was already migrated to a higher version.
	 */
	public static final String PROP_MIGRATED = "Migrated";  //$NON-NLS-1$

	/**
	 * Property: The list of supported platforms.
	 */
	public static final String PROP_PLATFORMS = "Platforms"; //$NON-NLS-1$

	/**
	 * Property: The connection subtype (real/sim/...).
	 */
	public static final String PROP_SUBTYPE= "SubType"; //$NON-NLS-1$

	/**
	 * Peer "SubType" attribute value for real target.
	 */
	public static final String SUBTYPE_REAL = "real"; //$NON-NLS-1$

	/**
	 * Peer "SubType" attribute value for simulator.
	 */
	public static final String SUBTYPE_SIM = "sim"; //$NON-NLS-1$

	/**
	 * Property: The connection mode (run/stop/...).
	 */
	public static final String PROP_MODE= "Mode"; //$NON-NLS-1$

	/**
	 * Peer "Mode" attribute value for application mode.
	 */
	public static final String MODE_RUN = "run"; //$NON-NLS-1$

	/**
	 * Peer "Mode" attribute value for stop mode.
	 */
	public static final String MODE_STOP = "stop"; //$NON-NLS-1$

	/**
	 * Property: The connection mode properties.
	 */
	public static final String PROP_MODE_PROPERTIES = "ModeProperties"; //$NON-NLS-1$

	/**
	 * Property: The kernel image.
	 */
	public static final String PROP_KERNEL_IMAGE= "KernelImage"; //$NON-NLS-1$

	/**
	 * Property: The peer type.
	 */
	public static final String PROP_TYPE = "Type"; //$NON-NLS-1$

	/**
	 * Property: List of TCF services the peer would have when it goes online (comma separated list).
	 */
	public static final String PROP_OFFLINE_SERVICES = "OfflineServices"; //$NON-NLS-1$

	/**
	 * Property: The peer visible state.
	 */
	public static final String PROP_VISIBLE = "Visible"; //$NON-NLS-1$

	/**
	 * Property: Simulator properties
	 */
	public static final String PROP_SIM_PROPERTIES = "SimulatorProperties"; //$NON-NLS-1$

	/**
	 * Property: Last selected simulator type
	 */
	public static final String PROP_SIM_TYPE = "SimulatorType"; //$NON-NLS-1$

	/**
	 * Property: Auto-start the debugger after the agent launch.
	 */
	public static final String PROP_AUTO_START_DEBUGGER = "autoStartDebugger"; //$NON-NLS-1$

	/**
	 * Property: Connect after the configuration has been created.
	 */
	public static final String PROP_AUTO_CONNECT = "autoConnect"; //$NON-NLS-1$

	/**
	 * Property: Ping interval.
	 */
	public static String PROP_PING_INTERVAL = "pingInterval"; //$NON-NLS-1$

	/**
	 * Property: Ping timeout.
	 */
	public static String PROP_PING_TIMEOUT = "pingTimeout"; //$NON-NLS-1$

	/**
	 * Default value for ping timeout property.
	 */
	public static String DEFAULT_PING_TIMEOUT = "2"; //$NON-NLS-1$

	/**
	 * The network port is a "auto port" (automatically determined and read-only to the user).
	 */
	public static final String PROP_IP_PORT_IS_AUTO = IWireTypeNetwork.PROPERTY_NETWORK_PORT_IS_AUTO;

	/**
	 * Property: "Recent Actions" history is supported or not.
	 */
	public static final String PROP_SUPPORTS_RECENT_ACTION_HISTORY = "SupportsRecentActionHistory"; //$NON-NLS-1$
}

