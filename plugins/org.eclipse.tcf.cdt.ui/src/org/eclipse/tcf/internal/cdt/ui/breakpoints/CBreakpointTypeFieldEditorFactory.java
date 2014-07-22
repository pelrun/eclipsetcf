/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.breakpoints;

import org.eclipse.cdt.debug.core.model.ICBreakpointType;
import org.eclipse.cdt.debug.ui.breakpoints.IFieldEditorFactory;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.services.IBreakpoints;


/**
 * HardwareFieldEditorFactory - Create the field editor for hardware breakpoint support.
 */
public class CBreakpointTypeFieldEditorFactory implements IFieldEditorFactory {

    public final static String NAME_HARDWARE = "org.eclipse.tcf.cdt.Hardware";
    public final static String NAME_TEMPORARY = "org.eclipse.tcf.cdt.Temporary";

    public final static String LABEL_HARDWARE = "Hardware";
    public final static String LABEL_TEMPORARY = "Temporary";

    public FieldEditor createFieldEditor(String name, String labelText, Composite parent) {
        if (NAME_HARDWARE.equals(name)) {
            return new CBreakpointTypeFieldEditor (parent, LABEL_HARDWARE, ICBreakpointType.HARDWARE, IBreakpoints.CAPABILITY_BREAKPOINT_TYPE);
        } else if (NAME_TEMPORARY.equals(name)) {
            return new CBreakpointTypeFieldEditor (parent, LABEL_TEMPORARY, ICBreakpointType.TEMPORARY, IBreakpoints.CAPABILITY_TEMPORARY);
        }
        return null;
    }


}
