/*******************************************************************************
 * Copyright (c) 2013, 2014 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.services.remote;

import java.util.Collection;
import java.util.Map;

import org.eclipse.tcf.core.Command;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IProfiler;

public class ProfilerProxy implements IProfiler {

    private final IChannel channel;

    public ProfilerProxy(IChannel channel) {
        this.channel = channel;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public IToken getCapabilities(String ctx, final DoneGetCapabilities done) {
        return new Command(channel, this, "getCapabilities", new Object[]{ ctx }) {
            @Override
            @SuppressWarnings("unchecked")
            public void done(Exception error, Object[] args) {
                Map<String,Object> capabilities = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    capabilities = (Map<String,Object>)args[1];
                }
                done.doneGetCapabilities(token, error, capabilities);
            }
        }.token;
    }

    @Override
    public IToken configure(String ctx, Map<String, Object> params, final DoneConfigure done) {
        return new Command(channel, this, "configure", new Object[]{ ctx, params }) {
            @Override
            public void done(Exception error, Object[] args) {
                if (error == null) {
                    assert args.length == 1;
                    error = toError(args[0]);
                }
                done.doneConfigure(token, error);
            }
        }.token;
    }

    @Override
    public IToken read(String ctx, final DoneRead done) {
        return new Command(channel, this, "read", new Object[]{ ctx }) {
            @Override
            public void done(Exception error, Object[] args) {
                Map<String,Object>[] data = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    data = toDataArray(args[1]);
                }
                done.doneRead(token, error, data);
            }
        }.token;
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object>[] toDataArray(Object o) {
        if (o == null) return null;
        Collection<Map<String,Object>> c = (Collection<Map<String,Object>>)o;
        return c.toArray(new Map[c.size()]);
    }
}
