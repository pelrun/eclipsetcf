/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import org.eclipse.tcf.internal.debug.ui.model.TCFNode;

public class ConsoleCommand extends AbstractActionDelegate {

    @Override
    protected void selectionChanged() {
        setEnabled(getSelectedNode() != null);
    }

    @Override
    protected void run() {
        TCFNode n = getSelectedNode();
        if (n != null) n.getModel().showDebugConsole();
    }
}
