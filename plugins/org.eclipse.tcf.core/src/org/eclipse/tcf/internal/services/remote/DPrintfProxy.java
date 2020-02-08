/*******************************************************************************
 * Copyright (c) 2013 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.services.remote;

import java.util.Map;

import org.eclipse.tcf.core.Command;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IDPrintf;

public class DPrintfProxy implements IDPrintf {

    private final IChannel channel;

    public DPrintfProxy(IChannel channel) {
        this.channel = channel;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public IToken open(Map<String,Object>[] properties, final DoneCommandOpen done) {
        return new Command(channel, this, "open", new Object[]{ properties }) {
            @Override
            public void done(Exception error, Object[] args) {
                String id = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    id = (String)args[1];
                }
                done.doneCommandOpen(token, error, id);
            }
        }.token;
    }

    @Override
    public IToken close(final DoneCommandClose done) {
        return new Command(channel, this, "close", null) {
            @Override
            public void done(Exception error, Object[] args) {
                if (error == null) {
                    assert args.length == 1;
                    error = toError(args[0]);
                }
                done.doneCommandClose(token, error);
            }
        }.token;
    }
}
