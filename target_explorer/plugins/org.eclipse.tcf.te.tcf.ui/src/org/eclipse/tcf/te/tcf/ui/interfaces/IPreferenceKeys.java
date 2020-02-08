/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.interfaces;

/**
 * Preference key identifiers.
 */
public interface IPreferenceKeys {
	/**
	 * Common prefix for all ui preference keys
	 */
	public final String PREFIX = "te.tcf.ui."; //$NON-NLS-1$

	/**
	 * The maximum number of recent actions shown in the recent action dialog.
	 * Defaults to 20.
	 */
	public final String PREF_MAX_RECENT_ACTION_ENTRIES = PREFIX + "maxRecentActions"; //$NON-NLS-1$

	/**
	 * Key prefix for auto connect setting in new wizards.
	 * The key needs to be followed by the connection type id.
	 */
	public final String PREF_AUTO_CONNECT = "autoConnect."; //$NON-NLS-1$
}
