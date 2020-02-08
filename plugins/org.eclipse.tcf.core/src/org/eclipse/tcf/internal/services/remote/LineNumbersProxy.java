/*******************************************************************************
 * Copyright (c) 2010, 2015 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.services.remote;

import java.util.Collection;
import java.util.Map;

import org.eclipse.tcf.core.Command;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.ILineNumbers;


public class LineNumbersProxy implements ILineNumbers {

    private final IChannel channel;

    public LineNumbersProxy(IChannel channel) {
        this.channel = channel;
    }

    public String getName() {
        return NAME;
    }

    public IToken mapToSource(String context_id, Number start_address,
            Number end_address, final DoneMapToSource done) {
        return new Command(channel, this, "mapToSource", new Object[]{ context_id,
                start_address, end_address }) {
            @Override
            public void done(Exception error, Object[] args) {
                CodeArea[] arr = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    arr = toTextAreaArray(args[1]);
                }
                done.doneMapToSource(token, error, arr);
            }
        }.token;
    }

    public IToken mapToMemory(String context_id, String file,
            int line, int column, final DoneMapToMemory done) {
        return new Command(channel, this, "mapToMemory", new Object[]{ context_id,
                file, line, column }) {
            @Override
            public void done(Exception error, Object[] args) {
                CodeArea[] arr = null;
                if (error == null) {
                    assert args.length == 2;
                    error = toError(args[0]);
                    arr = toTextAreaArray(args[1]);
                }
                done.doneMapToMemory(token, error, arr);
            }
        }.token;
    }

    @SuppressWarnings("unchecked")
    private CodeArea[] toTextAreaArray(Object o) {
        if (o == null) return null;
        Collection<Map<String,Object>> c = (Collection<Map<String,Object>>)o;
        int n = 0;
        CodeArea[] arr = new CodeArea[c.size()];
        for (Map<String,Object> area : c) {
            arr[n] = new CodeArea(area, n > 0 ? arr[n - 1] : null);
            n++;
        }
        return arr;
    }
}
