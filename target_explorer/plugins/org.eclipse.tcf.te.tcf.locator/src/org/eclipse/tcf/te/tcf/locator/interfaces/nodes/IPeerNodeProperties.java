/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
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
	 * Property: The redirection proxy peer id.
	 */
	public static final String PROP_REDIRECT_PROXY = "redirect.proxy"; //$NON-NLS-1$

	/**
	 * Property: Reference counter tracking the active channels for this peer.
	 */
	public static String PROP_CHANNEL_REF_COUNTER = "channelRefCounter.silent"; //$NON-NLS-1$

	/**
	 * Property: Discovered target for a static peer
	 */
	public static final String PROP_TARGET = "Target"; //$NON-NLS-1$

	/**
	 * Property: Exclude from scanner process. If set to <code>true</code>, the node will not be scanned
	 *           by the scanner.
	 */
	public static String PROP_SCANNER_EXCLUDE = "scanner.exclude.silent"; //$NON-NLS-1$

	/**
	 * Property: The last error the scanner encounter trying to open a channel to this peer.
	 */
	public static String PROP_LAST_SCANNER_ERROR = "lastScannerError"; //$NON-NLS-1$

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
