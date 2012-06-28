/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.breakpoints;

import org.eclipse.cdt.debug.ui.breakpoints.IFieldEditorFactory;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.widgets.Composite;


/**
 * HardwareFieldEditorFactory - Create the field editor for hardware breakpoint support.
 */
public class HardwareFieldEditorFactory implements IFieldEditorFactory {
    
    public FieldEditor createFieldEditor(String name, String labelText, Composite parent) {
        return new HardwareFieldEditor (parent);
    }
    
  
}