/*******************************************************************************
 * Copyright (c) 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.internal.examples.daytime;

import org.eclipse.tcf.core.Command;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;

/**
 * Daytime service proxy implementation.
 */
public class DaytimeServiceProxy implements IDaytimeService {

    private final IChannel channel;

    DaytimeServiceProxy(IChannel channel) {
        this.channel = channel;
    }

    /**
     * Return service name, as it appears on the wire - a TCF name of the service.
     */
    public String getName() {
        return NAME;
    }

    /**
     * The method translates arguments to JSON string and sends the command message
     * to remote server. When response arrives, it is translated from JSON to
     * Java object, which are used to call call-back object.
     *
     * The translation (marshaling) is done by using utility class Command.
     */
    public IToken getTimeOfDay(String tz, final DoneGetTimeOfDay done) {
        return new Command(channel, this, "getTimeOfDay", new Object[]{ tz }) {
            @Override
            public void done(Exception error, Object[] args) {
                String str = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    str = (String)args[1];
                }
                done.doneGetTimeOfDay(token, error, str);
            }
        }.token;
    }
}
