/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.breakpoints;

import org.eclipse.cdt.debug.core.model.ICBreakpointType;
import org.eclipse.cdt.debug.ui.breakpoints.IFieldEditorFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.services.IBreakpoints;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;


/**
 * CBreakpointTypeFieldEditorFactory - Create the field editor for hardware breakpoint support.
 */
public class CBreakpointTypeFieldEditorFactory implements IFieldEditorFactory {

    public final static String NAME_HARDWARE = "org.eclipse.tcf.cdt.Hardware";
    public final static String NAME_TEMPORARY = "org.eclipse.tcf.cdt.Temporary";

    public final static String LABEL_HARDWARE = "Hardware";
    public final static String LABEL_TEMPORARY = "Temporary";

    private final static boolean fgNeedContribution = needContribution();

    public FieldEditor createFieldEditor(String name, String labelText, Composite parent) {
        if (fgNeedContribution) {
            if (NAME_HARDWARE.equals(name)) {
                return new CBreakpointTypeFieldEditor (parent, LABEL_HARDWARE, ICBreakpointType.HARDWARE, IBreakpoints.CAPABILITY_BREAKPOINT_TYPE);
            } else if (NAME_TEMPORARY.equals(name)) {
                return new CBreakpointTypeFieldEditor (parent, LABEL_TEMPORARY, ICBreakpointType.TEMPORARY, IBreakpoints.CAPABILITY_TEMPORARY);
            }
        }
        return null;
    }

    private static boolean needContribution() {
        Bundle cdtDebugUi = Platform.getBundle("org.eclipse.cdt.debug.ui");
        if (cdtDebugUi != null) {
            int state = cdtDebugUi.getState();
            if (state == Bundle.ACTIVE || state == Bundle.RESOLVED || state == Bundle.STARTING) {
                Version version = cdtDebugUi.getVersion();
                // if cdt.debug.ui version is at least 7.4 (CDT 8.4) we don't need the contribution
                if (version.compareTo(Version.parseVersion("7.4")) >= 0)
                    return false;
            }
        }
        return true;
    }

}
