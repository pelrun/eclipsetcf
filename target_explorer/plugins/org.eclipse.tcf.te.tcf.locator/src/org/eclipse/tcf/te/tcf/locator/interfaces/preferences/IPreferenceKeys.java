/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces.preferences;

import java.io.File;

/**
 * The locator model bundle preference key identifiers.
 */
public interface IPreferenceKeys {
	/**
	 * Common prefix for all core preference keys
	 */
	public final String PREFIX = "te.tcf.locator.core."; //$NON-NLS-1$

	/**
	 * If set, the preference is defining a list of root locations where
	 * to lookup the static peer definitions. The single entries in the list
	 * are separated by the system dependent path separator character.
	 * <p>
	 * <b>Note:</b> If set, the given list of root locations replaces the default
	 * list of root locations.
	 *
	 * @see File#pathSeparatorChar
	 */
	public final String PREF_STATIC_PEERS_ROOT_LOCATIONS = PREFIX + "model.peers.rootLocations"; //$NON-NLS-1$

	/**
	 * Preference key to access the flag to hide value-add's in the "System Management" tree.
	 */
	public static final String PREF_HIDE_VALUEADDS = PREFIX + "model.valueadds.hide"; //$NON-NLS-1$
}
