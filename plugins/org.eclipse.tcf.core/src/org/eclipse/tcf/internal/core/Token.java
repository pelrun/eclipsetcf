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
package org.eclipse.tcf.internal.core;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;

/**
 * Implementation of the {@code IToken} interface. Used to match commands to results and to cancel pending commands
 */
public class Token implements IToken {

    /**
     * Internal static variable used to update the Token ID for the given command
     */
    private static int cnt = 0;

    /**
     * Token ID. This is what is seen when we
     */
    private final String id;
    /**
     * Byte representation of the Token ID
     */
    private final byte[] bytes;
    /**
     * ICommandListener associated with this token
     */
    private final IChannel.ICommandListener listener;

    public Token() {
        id = null;
        bytes = null;
        listener = null;
    }

    /**
     * Constructs a token with the given ICommandListener.
     * Used when sending a command/result to the channel
     * @param listener ICommandListener
     */
    public Token(IChannel.ICommandListener listener) {
        this.listener = listener;
        id = Integer.toString(cnt++);
        int l = id.length();
        bytes = new byte[l];
        for (int i = 0; i < l; i++) bytes[i] = (byte)id.charAt(i);
    }

    /**
     * Constructs a token from an array of bytes.
     * Used when receiving a command/result from the channel
     * @param bytes
     */
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
