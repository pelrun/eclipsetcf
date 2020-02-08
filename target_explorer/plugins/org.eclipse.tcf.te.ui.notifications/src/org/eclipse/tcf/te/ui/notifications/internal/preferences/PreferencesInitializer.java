/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.notifications.internal.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.tcf.te.runtime.preferences.ScopedEclipsePreferences;
import org.eclipse.tcf.te.ui.notifications.activator.UIPlugin;
import org.eclipse.tcf.te.ui.notifications.interfaces.IPreferenceKeys;


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

		// Notification fading animation: default on
		store.putDefaultBoolean(IPreferenceKeys.PREF_ENABLE_FADING, true);
	}
}
