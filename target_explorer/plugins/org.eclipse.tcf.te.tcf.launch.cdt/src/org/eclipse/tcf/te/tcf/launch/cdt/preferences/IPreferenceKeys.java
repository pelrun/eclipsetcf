/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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
	 * The default (remote) gdbserver port (String).
	 */
	public static final String PREF_GDBSERVER_PORT = PREFIX + ".gdbserver.port"; //$NON-NLS-1$

	/**
	 * The default (local) gdbserver port (String).
	 */
	public static final String PREF_GDBSERVER_PORT_MAPPED_TO = PREFIX + ".gdbserver.portMappedTo"; //$NON-NLS-1$

}
