/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class TCFPreferencesInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore prefs = TCFPreferences.getPreferenceStore();

        prefs.setDefault(TCFPreferences.PREF_STACK_FRAME_LIMIT_ENABLED, true);
        prefs.setDefault(TCFPreferences.PREF_STACK_FRAME_LIMIT_VALUE, 10);
        prefs.setDefault(TCFPreferences.PREF_STACK_FRAME_ARG_NAMES, false);
        prefs.setDefault(TCFPreferences.PREF_STACK_FRAME_ARG_VALUES, false);
        prefs.setDefault(TCFPreferences.PREF_WAIT_FOR_PC_UPDATE_AFTER_STEP, true);
        prefs.setDefault(TCFPreferences.PREF_WAIT_FOR_VIEWS_UPDATE_AFTER_STEP, false);
        prefs.setDefault(TCFPreferences.PREF_DELAY_STACK_UPDATE_UNTIL_LAST_STEP, false);
        prefs.setDefault(TCFPreferences.PREF_MIN_STEP_INTERVAL, 50);
        prefs.setDefault(TCFPreferences.PREF_MIN_UPDATE_INTERVAL, 50);
        prefs.setDefault(TCFPreferences.PREF_VIEW_UPDATES_THROTTLE, true);
        prefs.setDefault(TCFPreferences.PREF_TARGET_TRAFFIC_THROTTLE, true);
        prefs.setDefault(TCFPreferences.PREF_AUTO_CHILDREN_LIST_UPDATES, true);
        prefs.setDefault(TCFPreferences.PREF_DELAY_CHILDREN_LIST_UPDATES, false);
        prefs.setDefault(TCFPreferences.PREF_FULL_ERROR_REPORTS, false);
        prefs.setDefault(TCFPreferences.PREF_HOVER_WHILE_RUNNING, false);
        prefs.setDefault(TCFPreferences.PREF_SHOW_QUALIFIED_TYPE_NAMES, false);
        prefs.setDefault(TCFPreferences.PREF_FILTER_VARIANTS_BY_DISCRIMINANT, false);
    }
}
