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

import org.eclipse.cdt.debug.ui.breakpoints.IFieldEditorFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.widgets.Composite;


/**
 * Create field editors to edit the read/write properties of a watchpoint.
 */
public class CWatchpointFieldEditorFactory implements IFieldEditorFactory {

    private static final String READ_ID = "org.eclipse.cdt.debug.core.read";
    private static final String WRITE_ID = "org.eclipse.cdt.debug.core.write";

    public FieldEditor createFieldEditor(String name, String labelText, Composite parent) {
        if (READ_ID.equals(name) || WRITE_ID.equals(name)) {
            return new BooleanFieldEditor(name, labelText, parent);
        }
        return null;
    }


}
