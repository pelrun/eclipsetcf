/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal.preferences;

/**
 * The constants for the preferences.
 */
public interface IPreferenceKeys {
	/**
	 * Common prefix for all preference keys
	 */
	public final String PREFIX = "te.tcf.ui."; //$NON-NLS-1$

	/**
	 * Preference key to access the flag to hide dynamic target discovery content extension.
	 */
	public static final String PREF_HIDE_DYNAMIC_TARGET_DISCOVERY_EXTENSION = "org.eclipse.tcf.te.tcf.ui.navigator.content.hide"; //$NON-NLS-1$

	/**
	 * Preference key to access the flag to activate the current user filter on first launch.
	 */
	public static final String PREF_ACTIVATE_CURRENT_USER_FILTER = PREFIX + "model.currentUserFilter.activate"; //$NON-NLS-1$

	/**
	 * Preference key to allow target path for module load.
	 */
	public static final String PREF_OPEN_EDITOR_ON_DEFAULT_CONTEXT_CHANGE = "org.eclipse.tcf.te.tcf.ui.defaultcontext.open.editor"; //$NON-NLS-1$
}
