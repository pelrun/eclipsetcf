/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.jface.window.Window;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate;
import org.eclipse.tcf.internal.debug.ui.launch.ContextSelection;
import org.eclipse.tcf.internal.debug.ui.launch.ContextSelectionDialog;
import org.eclipse.ui.PlatformUI;

public class ProcessPrompter implements IStatusHandler {

    public Object handleStatus(IStatus status, Object source) throws CoreException {
        ILaunchConfiguration config = (ILaunchConfiguration) source;
        String peerId = config.getAttribute(TCFLaunchDelegate.ATTR_PEER_ID, (String)null);
        String contextPath = config.getAttribute(TCFLaunchDelegate.ATTR_ATTACH_PATH, (String)null);
        if (peerId == null || contextPath == null) {
            ContextSelection selection = new ContextSelection();
            selection.fPeerId = peerId;
            selection.fContextFullName = contextPath;
            ContextSelectionDialog diag = new ContextSelectionDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), true);
            diag.setSelection(selection);
            if (diag.open() == Window.OK) {
                return diag.getSelection();
            }
        }
        return null;
    }
}
