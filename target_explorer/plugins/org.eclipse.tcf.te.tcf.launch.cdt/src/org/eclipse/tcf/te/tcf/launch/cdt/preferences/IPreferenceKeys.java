/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.preferences;

import org.eclipse.tcf.te.tcf.launch.cdt.activator.Activator;


/**
 * The CDT launch configuration extensions preference key identifiers.
 */
public interface IPreferenceKeys {
	/**
	 * Common prefix for all core preference keys
	 */
	public final String PREFIX = Activator.getUniqueIdentifier();

	/**
	 * The default gdbserver command (String).
	 */
	public static final String PREF_GDBSERVER_COMMAND = PREFIX + ".gdbserver.command"; //$NON-NLS-1$

	/**
	 * The default GDB initialization file (String).
	 */
	public static final String PREF_GDB_INIT = PREFIX + ".gdbserver.init"; //$NON-NLS-1$

	/**
	 * The default (remote) gdbserver port (String).
	 */
	public static final String PREF_GDBSERVER_PORT = PREFIX + ".gdbserver.port"; //$NON-NLS-1$

	/**
	 * The default (local) gdbserver port (String).
	 */
	public static final String PREF_GDBSERVER_PORT_MAPPED_TO = PREFIX + ".gdbserver.portMappedTo"; //$NON-NLS-1$

	/**
	 * The list of alternate (remote) gdbserver port (String, comma-separated).
	 */
	public static final String PREF_GDBSERVER_PORT_ALTERNATIVES = PREFIX + ".gdbserver.port.alternatives"; //$NON-NLS-1$

	/**
	 * The list of alternate (local) gdbserver port (String, comma-separated).
	 */
	public static final String PREF_GDBSERVER_PORT_MAPPED_TO_ALTERNATIVES = PREFIX + ".gdbserver.portMappedTo.alternatives"; //$NON-NLS-1$

	/**
	 * The default gdbserver command (String) (attach).
	 */
	public static final String PREF_GDBSERVER_COMMAND_ATTACH = PREFIX + ".gdbserver.command.attach"; //$NON-NLS-1$

	/**
	 * The default (remote) gdbserver port (String) (attach).
	 */
	public static final String PREF_GDBSERVER_PORT_ATTACH = PREFIX + ".gdbserver.port.attach"; //$NON-NLS-1$

	/**
	 * The default (local) gdbserver port (String) (attach).
	 */
	public static final String PREF_GDBSERVER_PORT_ATTACH_MAPPED_TO = PREFIX + ".gdbserver.portMappedTo.attach"; //$NON-NLS-1$

	/**
	 * The list of alternate (remote) gdbserver port (String, comma-separated) (attach).
	 */
	public static final String PREF_GDBSERVER_PORT_ATTACH_ALTERNATIVES = PREFIX + ".gdbserver.port.attach.alternatives"; //$NON-NLS-1$

	/**
	 * The list of alternate (local) gdbserver port (String, comma-separated) (attach).
	 */
	public static final String PREF_GDBSERVER_PORT_ATTACH_MAPPED_TO_ALTERNATIVES = PREFIX + ".gdbserver.portMappedTo.attach.alternatives"; //$NON-NLS-1$
}
