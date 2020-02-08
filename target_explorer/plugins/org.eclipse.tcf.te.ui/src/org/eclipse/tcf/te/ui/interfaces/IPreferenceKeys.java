/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.interfaces;

/**
 * The bundle's preference key identifiers.
 */
public interface IPreferenceKeys {
	/**
	 * Common prefix for all UI preference keys
	 */
	public final String PREFIX = "te.ui."; //$NON-NLS-1$

	/**
	 * Preference key to access the option that if the search is a DFS.
	 */
	public static final String PREF_DEPTH_FIRST_SEARCH = "PrefDFS"; //$NON-NLS-1$

	/**
	 * Preference key to access the flag controlling if configuration editors are persisted on session close
	 */
	public static final String PREF_PERSIST_EDITORS = PREFIX + "persistEditors"; //$NON-NLS-1$
}
