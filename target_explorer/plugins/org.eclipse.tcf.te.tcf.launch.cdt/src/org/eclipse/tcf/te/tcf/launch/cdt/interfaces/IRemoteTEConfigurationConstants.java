/*******************************************************************************
 * Copyright (c) 2006, 2018 PalmSource, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Ewa Matejska    (PalmSource) - Adapted from IGDBServerMILaunchConfigurationConstants
 * Anna Dushistova (MontaVista) - [181517][usability] Specify commands to be run before remote application launch
 * Anna Dushistova (MontaVista) - cloned from IRemoteConnectionConfigurationConstants
 * Alexandru Dragut             - [535867] Make Path Mapping configurable in TCF Launch config UI
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.interfaces;

import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.debug.core.DebugPlugin;

public interface IRemoteTEConfigurationConstants {

	public static final String ATTR_REMOTE_CONNECTION = DebugPlugin.getUniqueIdentifier() + ".REMOTE_TCP"; //$NON-NLS-1$

	public static final String ATTR_GDBSERVER_PORT = DebugPlugin.getUniqueIdentifier() + ".ATTR_GDBSERVER_PORT"; //$NON-NLS-1$
	public static final String ATTR_GDBSERVER_PORT_MAPPED_TO = DebugPlugin.getUniqueIdentifier() + ".ATTR_GDBSERVER_PORT_MAPPED_TO"; //$NON-NLS-1$
	public static final String ATTR_GDBSERVER_COMMAND = DebugPlugin.getUniqueIdentifier() + ".ATTR_GDBSERVER_COMMAND"; //$NON-NLS-1$
	public static final String ATTR_GDB_INIT = IGDBLaunchConfigurationConstants.ATTR_GDB_INIT + "_NEW"; //$NON-NLS-1$
	public static final String ATTR_GDBSERVER_PORT_ALTERNATIVES = DebugPlugin.getUniqueIdentifier() + ".ATTR_GDBSERVER_PORT_ALTERNATIVES"; //$NON-NLS-1$
	public static final String ATTR_GDBSERVER_PORT_MAPPED_TO_ALTERNATIVES = DebugPlugin.getUniqueIdentifier() + ".ATTR_GDBSERVER_PORT_MAPPED_TO_ALTERNATIVES"; //$NON-NLS-1$

	/*
	 * Generic Remote Path and Download options:
	 *     ATTR_REMOTE_PATH: Path of the binary on the remote.
	 *     ATTR_SKIP_DOWNLOAD_TO_TARGET: true if download to remote is not desired.
	 */
	public static final String ATTR_REMOTE_PATH = DebugPlugin.getUniqueIdentifier() + ".ATTR_TARGET_PATH"; //$NON-NLS-1$
	public static final String ATTR_SKIP_DOWNLOAD_TO_TARGET = DebugPlugin.getUniqueIdentifier() + ".ATTR_SKIP_DOWNLOAD_TO_TARGET"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key. Boolean value to set the auto path mapping property
	 * from local object path to remote object path.
	 * If the attribute is not set, the default value of this attribute is true.
	 * @since 1.7
	 */
	public static final String ATTR_AUTO_PATH_MAPPING_FROM_LOCAL_TO_REMOTE = DebugPlugin.getUniqueIdentifier() + ".ATTR_AUTO_PATH_MAPPING_FROM_LOCAL_TO_REMOTE"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute key. Boolean value to set the auto path mapping property
	 * from remote object path to local object path.
	 * If the attribute is not set, the default value of this attribute is true.
	 * @since 1.7
	 */
	public static final String ATTR_AUTO_PATH_MAPPING_FROM_REMOTE_TO_LOCAL = DebugPlugin.getUniqueIdentifier() + ".ATTR_AUTO_PATH_MAPPING_FROM_REMOTE_TO_LOCAL"; //$NON-NLS-1$

	/*
	 * The remote PID to attach to.
	 */
	public static final String ATTR_REMOTE_PID = DebugPlugin.getUniqueIdentifier() + ".ATTR_REMOTE_PID"; //$NON-NLS-1$

	public static final String ATTR_PRERUN_COMMANDS = DebugPlugin.getUniqueIdentifier() + ".ATTR_PRERUN_CMDS"; //$NON-NLS-1$

	public static final boolean ATTR_SKIP_DOWNLOAD_TO_TARGET_DEFAULT = false;

	public static final String ATTR_REMOTE_USER_ID = DebugPlugin.getUniqueIdentifier() + ".ATTR_REMOTE_USER_ID"; //$NON-NLS-1$
	public static final String ATTR_LAUNCH_REMOTE_USER = DebugPlugin.getUniqueIdentifier() + ".ATTR_LAUNCH_REMOTE_USER"; //$NON-NLS-1$
	public static final boolean ATTR_LAUNCH_REMOTE_USER_DEFAULT = false;


}