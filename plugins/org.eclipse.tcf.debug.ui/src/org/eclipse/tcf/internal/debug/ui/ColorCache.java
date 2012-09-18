/*******************************************************************************
 * Copyright (c) 2012 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.RGB;

public class ColorCache {

    public static final RGB
        rgb_error = new RGB(192, 0, 0),
        rgb_disabled = new RGB(127, 127, 127),
        rgb_stalled = new RGB(128, 128, 128);

    public static RGB rgb_highlight =
        DebugUITools.getPreferenceColor(IDebugUIConstants.PREF_CHANGED_VALUE_BACKGROUND).getRGB();

    private static final IPropertyChangeListener preferenceChangeListener = new IPropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (event.getProperty().equals(IDebugUIConstants.PREF_CHANGED_VALUE_BACKGROUND)) {
                rgb_highlight = DebugUITools.getPreferenceColor(IDebugUIConstants.PREF_CHANGED_VALUE_BACKGROUND).getRGB();
            }
        }
    };

    static {
        DebugUITools.getPreferenceStore().addPropertyChangeListener(preferenceChangeListener);
    }
}
