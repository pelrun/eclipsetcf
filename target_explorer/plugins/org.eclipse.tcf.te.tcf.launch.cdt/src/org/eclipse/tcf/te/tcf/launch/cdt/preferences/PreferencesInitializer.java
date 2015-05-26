/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.tcf.te.runtime.preferences.ScopedEclipsePreferences;
import org.eclipse.tcf.te.tcf.launch.cdt.activator.Activator;

/**
 * Launch core framework preferences initializer implementation.
 */
public class PreferencesInitializer extends AbstractPreferenceInitializer {

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		// Get the preferences store
		ScopedEclipsePreferences store = Activator.getScopedPreferences();

		/**
		 * Gdbserver default command: gdbserver
		 */
		store.putDefaultString(IPreferenceKeys.PREF_GDBSERVER_PORT, "gdbserver"); //$NON-NLS-1$

		/**
		 * Gdbserver default (remote) port: 2345
		 */
		store.putDefaultString(IPreferenceKeys.PREF_GDBSERVER_PORT, "2345"); //$NON-NLS-1$

		/**
		 * Gdbserver default (local) port: N/A
		 */
		store.putDefaultString(IPreferenceKeys.PREF_GDBSERVER_PORT, null);
	}
}
