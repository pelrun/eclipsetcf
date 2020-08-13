/*******************************************************************************
 * Copyright (c) 2011-2020 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IColumnPresentation;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Column presentation for the Modules view.
 */
public class TCFColumnPresentationModules implements IColumnPresentation {

    public static final String PRESENTATION_ID = "Modules";

    /**
     * Presentation column IDs.
     */
    public static final String
        COL_NAME = "Name",
        COL_FILE = "File",
        COL_ADDRESS = "Address",
        COL_SIZE = "Size",
        COL_FLAGS = "Flags",
        COL_OFFSET = "Offset",
        COL_SECTION = "Section";

    private static String[] cols_all = {
        COL_NAME,
        COL_FILE,
        COL_ADDRESS,
        COL_SIZE,
        COL_FLAGS,
        COL_OFFSET,
        COL_SECTION
    };

    private static String[] headers  = {
        "Name",
        "File Name",
        "Address",
        "Size",
        "Flags",
        "Offset",
        "Section"
    };

    private static String[] cols_ini = {
        COL_NAME,
        COL_FILE,
        COL_ADDRESS,
        COL_SIZE
    };

    public void dispose() {
    }

    public String[] getAvailableColumns() {
        return cols_all;
    }

    public String getHeader(String id) {
        for (int i = 0; i < cols_all.length; i++) {
            if (id.equals(cols_all[i])) return headers[i];
        }
        return null;
    }

    public String getId() {
        return PRESENTATION_ID;
    }

    public ImageDescriptor getImageDescriptor(String id) {
        return null;
    }

    public String[] getInitialColumns() {
        return cols_ini;
    }

    public void init(IPresentationContext context) {
    }

    public boolean isOptional() {
        return true;
    }
}
