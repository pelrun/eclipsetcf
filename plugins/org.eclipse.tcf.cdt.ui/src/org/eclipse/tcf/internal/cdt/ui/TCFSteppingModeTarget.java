/*******************************************************************************
 * Copyright (c) 2010-2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui;

import org.eclipse.cdt.debug.core.model.ISteppingModeTarget;
import org.eclipse.cdt.debug.core.model.ITargetProperties;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;

/**
 * Integrates the TCF model with the "Instruction Stepping Mode" button from CDT.
 */
@SuppressWarnings("deprecation")
public class TCFSteppingModeTarget implements ISteppingModeTarget, ITargetProperties {

    private final Preferences prefs;
    private final TCFModel model;

    public TCFSteppingModeTarget(TCFModel model) {
        prefs= new Preferences();
        prefs.setDefault(PREF_INSTRUCTION_STEPPING_MODE, model.isInstructionSteppingEnabled());
        this.model = model;
    }

    public void addPropertyChangeListener(IPropertyChangeListener listener) {
        prefs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(IPropertyChangeListener listener) {
        prefs.removePropertyChangeListener(listener);
    }

    public boolean supportsInstructionStepping() {
        return true;
    }

    public void enableInstructionStepping(boolean enabled) {
        prefs.setValue(PREF_INSTRUCTION_STEPPING_MODE, enabled);
        model.setInstructionSteppingEnabled(enabled);
    }

    public boolean isInstructionSteppingEnabled() {
        return prefs.getBoolean(PREF_INSTRUCTION_STEPPING_MODE);
    }
}
