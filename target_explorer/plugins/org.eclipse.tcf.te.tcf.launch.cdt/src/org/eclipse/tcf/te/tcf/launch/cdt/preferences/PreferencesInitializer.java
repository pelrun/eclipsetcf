/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
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
		store.putDefaultString(IPreferenceKeys.PREF_GDBSERVER_COMMAND, "gdbserver"); //$NON-NLS-1$

		/**
		 * GDB initialization file: ${system_property:user.home}/.gdbinit
		 */
		store.putDefaultString(IPreferenceKeys.PREF_GDB_INIT, "${system_property:user.home}/.gdbinit"); //$NON-NLS-1$

		/**
		 * Gdbserver default (remote) port: 2345
		 */
		store.putDefaultString(IPreferenceKeys.PREF_GDBSERVER_PORT, "2345"); //$NON-NLS-1$

		/**
		 * Gdbserver (remote) port alternatives: N/A
		 */
		store.putDefaultString(IPreferenceKeys.PREF_GDBSERVER_PORT_ALTERNATIVES, null);

		/**
		 * Gdbserver default (local) port: N/A
		 */
		store.putDefaultString(IPreferenceKeys.PREF_GDBSERVER_PORT_MAPPED_TO, null);

		/**
		 * Gdbserver (local) port alternatives: N/A
		 */
		store.putDefaultString(IPreferenceKeys.PREF_GDBSERVER_PORT_MAPPED_TO_ALTERNATIVES, null);

		/**
		 * Gdbserver default command (attach): gdbserver
		 */
		store.putDefaultString(IPreferenceKeys.PREF_GDBSERVER_COMMAND_ATTACH, "gdbserver"); //$NON-NLS-1$

		/**
		 * Gdbserver default (remote) port (attach): 2345
		 */
		store.putDefaultString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH, "2345"); //$NON-NLS-1$

		/**
		 * Gdbserver (remote) port alternatives (attach): N/A
		 */
		store.putDefaultString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_ALTERNATIVES, null);

		/**
		 * Gdbserver default (local) port (attach): N/A
		 */
		store.putDefaultString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_MAPPED_TO, null);

		/**
		 * Gdbserver (local) port alternatives (attach): N/A
		 */
		store.putDefaultString(IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_MAPPED_TO_ALTERNATIVES, null);
	}
}
