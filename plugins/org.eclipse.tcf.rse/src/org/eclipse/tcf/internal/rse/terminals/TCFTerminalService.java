/*******************************************************************************
 * Copyright (c) 2010, 2011 Intel Corporation. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Liping Ke(Intel Corp.) - initial API and implementation
 ******************************************************************************/
package org.eclipse.tcf.internal.rse.terminals;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.terminals.AbstractTerminalService;
import org.eclipse.rse.services.terminals.ITerminalShell;
import org.eclipse.tcf.internal.rse.ITCFService;
import org.eclipse.tcf.internal.rse.ITCFSessionProvider;
import org.eclipse.tcf.internal.rse.Messages;
import org.eclipse.tcf.internal.rse.shells.TCFTerminalShell;

public class TCFTerminalService extends AbstractTerminalService implements ITCFService{
    private final ITCFSessionProvider fSessionProvider;

    public ITerminalShell launchTerminal(String ptyType, String encoding,
            String[] environment, String initialWorkingDirectory,
            String commandToRun, IProgressMonitor monitor)
            throws SystemMessageException {
        return new TCFTerminalShell(fSessionProvider, ptyType, encoding,
                environment, initialWorkingDirectory, commandToRun);
    }

    public TCFTerminalService(ITCFSessionProvider sessionProvider) {
        fSessionProvider = sessionProvider;
    }

    public ITCFSessionProvider getSessionProvider() {
        return fSessionProvider;
    }

    @Override
    public String getName() {
        return Messages.TCFTerminalService_Name;
    }

    @Override
    public String getDescription() {
        return Messages.TCFTerminalService_Description;
    }
}
