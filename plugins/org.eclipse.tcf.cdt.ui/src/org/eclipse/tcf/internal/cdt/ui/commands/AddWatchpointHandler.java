/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.internal.cdt.ui.commands;

import org.eclipse.cdt.debug.ui.CDebugUIPlugin;
import org.eclipse.cdt.debug.ui.breakpoints.IToggleBreakpointsTargetCExtension;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class AddWatchpointHandler extends AbstractHandler {
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        IWorkbenchPart part = HandlerUtil.getActivePartChecked(event);
        IToggleBreakpointsTarget toggleTarget = DebugUITools.getToggleBreakpointsTargetManager().getToggleBreakpointsTarget(part, selection);
        IToggleBreakpointsTargetCExtension cToggleTarget = null;
        if (toggleTarget instanceof IToggleBreakpointsTargetCExtension) {
            cToggleTarget = (IToggleBreakpointsTargetCExtension)toggleTarget;
        }
        else {
            CDebugUIPlugin.errorDialog("Cannot add watchpoint.", (Throwable) null);
            return null;
        }

        try {
            cToggleTarget.createWatchpointsInteractive(part, selection);
        }
        catch (CoreException e) {
            CDebugUIPlugin.errorDialog("Cannot add watchpoint.", e);
        }
        return null;
    }
}
