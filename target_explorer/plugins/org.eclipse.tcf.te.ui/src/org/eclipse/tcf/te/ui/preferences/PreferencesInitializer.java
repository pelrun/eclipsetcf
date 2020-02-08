/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.tcf.te.runtime.preferences.ScopedEclipsePreferences;
import org.eclipse.tcf.te.ui.activator.UIPlugin;
import org.eclipse.tcf.te.ui.interfaces.IPreferenceKeys;


/**
 * The bundle's preference initializer implementation.
 */
public class PreferencesInitializer extends AbstractPreferenceInitializer implements IPreferenceKeys {

	/**
	 * Constructor.
	 */
	public PreferencesInitializer() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		ScopedEclipsePreferences store = UIPlugin.getScopedPreferences();

		// Search is a DFS: default off
		store.putDefaultBoolean(PREF_DEPTH_FIRST_SEARCH, false);
		// Persist editors: default on
		store.putDefaultBoolean(IPreferenceKeys.PREF_PERSIST_EDITORS, true);

	}
}
