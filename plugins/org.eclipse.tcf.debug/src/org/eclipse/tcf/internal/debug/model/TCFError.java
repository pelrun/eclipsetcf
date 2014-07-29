/*******************************************************************************
 * Copyright (c) 2007, 2013 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.model;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tcf.internal.debug.Activator;


public class TCFError implements IStatus {

    private final Throwable exception;

    public TCFError(Throwable exception) {
        this.exception = exception;
    }

    public IStatus[] getChildren() {
        return null;
    }

    public int getCode() {
        return 1;
    }

    public Throwable getException() {
        return exception;
    }

    public String getMessage() {
        return exception.getMessage();
    }

    public String getPlugin() {
        return Activator.PLUGIN_ID;
    }

    public int getSeverity() {
        return ERROR;
    }

    public boolean isMultiStatus() {
        return false;
    }

    public boolean isOK() {
        return false;
    }

    public boolean matches(int severityMask) {
        return false;
    }
}
