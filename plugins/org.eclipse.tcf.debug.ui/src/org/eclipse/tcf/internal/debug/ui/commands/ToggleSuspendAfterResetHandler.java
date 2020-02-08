/*******************************************************************************
 * Copyright (c) 2019.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.tcf.internal.debug.ui.preferences.TCFPreferences;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

public class ToggleSuspendAfterResetHandler extends AbstractHandler implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        boolean state = !HandlerUtil.toggleCommandState(event.getCommand());
        TCFPreferences.getPreferenceStore().setValue(TCFPreferences.PREF_SUSPEND_AFTER_RESET, state);
        return null;
    }

    @Override
    public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
        element.setChecked(TCFPreferences.getPreferenceStore().getBoolean(TCFPreferences.PREF_SUSPEND_AFTER_RESET));
    }
}
