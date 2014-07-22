/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.core;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;


public class Token implements IToken {

    private static int cnt = 0;

    private final String id;
    private final byte[] bytes;
    private final IChannel.ICommandListener listener;

    public Token() {
        id = null;
        bytes = null;
        listener = null;
    }

    public Token(IChannel.ICommandListener listener) {
        this.listener = listener;
        id = Integer.toString(cnt++);
        int l = id.length();
        bytes = new byte[l];
        for (int i = 0; i < l; i++) bytes[i] = (byte)id.charAt(i);
    }

    public Token(byte[] bytes) {
        this.bytes = bytes;
        listener = null;
        int l = bytes.length;
        char[] bf = new char[l];
        for (int i = 0; i < l; i++) bf[i] = (char)(bytes[i] & 0xff);
        id = new String(bf);
    }

    public boolean cancel() {
        return false;
    }

    public String getID() {
        return id;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public IChannel.ICommandListener getListener() {
        return listener;
    }

    @Override
    public String toString() {
        return id;
    }
}
