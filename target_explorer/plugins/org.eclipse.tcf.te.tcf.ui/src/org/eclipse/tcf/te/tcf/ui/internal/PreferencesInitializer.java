/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.tcf.te.runtime.preferences.ScopedEclipsePreferences;
import org.eclipse.tcf.te.tcf.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.ui.interfaces.IPreferenceKeys;

/**
 * Preference initializer.
 */
public class PreferencesInitializer extends AbstractPreferenceInitializer {

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
		// Get the bundles scoped preferences store
		ScopedEclipsePreferences prefs = UIPlugin.getScopedPreferences();
		if (prefs != null) {
			prefs.putDefaultInt(IPreferenceKeys.PREF_MAX_RECENT_ACTION_ENTRIES, 20);
		}
	}
}
