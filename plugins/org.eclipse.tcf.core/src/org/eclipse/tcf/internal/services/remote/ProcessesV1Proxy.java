/*******************************************************************************
 * Copyright (c) 2007, 2013 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.services.remote;

import java.util.Map;

import org.eclipse.tcf.core.Command;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IProcessesV1;

public class ProcessesV1Proxy extends ProcessesProxy implements IProcessesV1 {

    public ProcessesV1Proxy(IChannel channel) {
        super(channel);
    }

    public String getName() {
        return IProcessesV1.NAME;
    }

    @Override
    public IToken start(String directory, String file,
            String[] command_line, Map<String,String> environment,
            Map<String,Object> params, final DoneStart done) {
        return new Command(channel, this,
                "start", new Object[]{ directory, file, command_line, //$NON-NLS-1$
                toEnvStringArray(environment), params }) {
            @SuppressWarnings("unchecked")
            @Override
            public void done(Exception error, Object[] args) {
                ProcessContext ctx = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    if (args[1] != null) ctx = new ProcessContextInfo((Map<String,Object>)args[1]);
                }
                done.doneStart(token, error, ctx);
            }
        }.token;
    }

    @Override
    public IToken setWinSize(String id, int col, int row, final DoneCommand done) {
        return new Command(channel, this, "setWinSize", new Object[]{ id, col, row }) { //$NON-NLS-1$
            @Override
            public void done(Exception error, Object[] args) {
                if (error == null) {
                    assert args.length == 1;
                    error = toError(args[0]);
                }
                done.doneCommand(token, error);
            }
        }.token;
    }

    @Override
    public IToken getCapabilities(final String id, final DoneGetCapabilities done) {
        return new Command(channel, this, "getCapabilities", new Object[]{ id }) { //$NON-NLS-1$
            @SuppressWarnings("unchecked")
            @Override
            public void done(Exception error, Object[] args) {
                Map<String, Object> properties = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    if (args[1] != null) {
                        properties = (Map<String, Object>)args[1];
                    }
                }
                done.doneGetCapabilities(token, error, properties);
            }
        }.token;
    }
}
