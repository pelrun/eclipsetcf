/*******************************************************************************
 * Copyright (c) 2019.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *******************************************************************************/
package org.eclipse.tcf.internal.services.remote;

import java.util.Collection;
import java.util.Map;

import org.eclipse.tcf.core.Command;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IContextReset;

public class ContextResetProxy implements IContextReset {

    private IChannel channel;

    public ContextResetProxy(IChannel channel) {
        this.channel = channel;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public IToken getCapabilities(String context_id, final DoneGetCapabilities done) {
        return new Command(channel, this, "getCapabilities", new Object[] { context_id }) {
            @SuppressWarnings("unchecked")
            @Override
            public void done(Exception error, Object[] args) {
                Collection<Map<String, Object>> map = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    map = (Collection<Map<String, Object>>) args[1];
                }
                done.doneGetCapabilities(token, error, map);
            }
        }.token;
    }

    @Override
    public IToken reset(String context_id, String reset_type, Map<String, Object> params, final DoneReset done) {
        return new Command(channel, this, "reset", new Object[] { context_id, reset_type, params }) {
            @Override
            public void done(Exception error, Object[] args) {
                if (error == null) {
                    assert args.length == 1;
                    error = toError(args[0]);
                }
                done.doneReset(token, error);
            }
        }.token;
    }
}
