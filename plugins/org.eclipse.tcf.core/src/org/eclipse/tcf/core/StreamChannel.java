/*******************************************************************************
 * Copyright (c) 2007-2019 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.core;

import java.io.IOException;

import org.eclipse.tcf.protocol.IPeer;

/**
 * Abstract implementation of IChannel interface for stream oriented transport protocols.
 *
 * StreamChannel implements communication link connecting two end points (peers).
 * The channel asynchronously transmits messages: commands, results and events.
 *
 * StreamChannel uses escape sequences to represent End-Of-Message and End-Of-Stream markers.
 *
 * Clients can subclass StreamChannel to support particular stream oriented transport (wire) protocol.
 * Also, see ChannelTCP for a concrete IChannel implementation that works on top of TCP sockets as a transport.
 */
public abstract class StreamChannel extends AbstractChannel {

    public static final int ESC = 3;

    private int bin_data_size;

    private final byte[] esc_buf = new byte[0x1000];

    private final byte[] inp_buf = new byte[0x4000];
    private int inp_buf_pos;
    private int inp_buf_len;

    public StreamChannel(IPeer remote_peer) {
        super(remote_peer);
    }

    public StreamChannel(IPeer local_peer, IPeer remote_peer) {
        super(local_peer, remote_peer);
    }

    protected abstract int get() throws IOException;
    protected abstract void put(int n) throws IOException;

    protected int get(byte[] buf) throws IOException {
        /* Default implementation - it is expected to be overridden */
        int i = 0;
        while (i < buf.length) {
            int b = get();
            if (b < 0) {
                if (i == 0) return -1;
                break;
            }
            buf[i++] = (byte)b;
            if (i >= bin_data_size) break;
        }
        return i;
    }

    protected void put(byte[] buf) throws IOException {
        /* Default implementation - it is expected to be overridden */
        for (byte b : buf) put(b & 0xff);
    }

    /**
     * @since 1.3
     */
    protected void put(byte[] buf, int pos, int len) throws IOException {
        /* Default implementation - it is expected to be overridden */
        int end = pos + len;
        while (pos < end) put(buf[pos++] & 0xff);
    }

    @Override
    protected final int read() throws IOException {
        for (;;) {
            while (inp_buf_pos >= inp_buf_len) {
                inp_buf_len = get(inp_buf);
                inp_buf_pos = 0;
                if (inp_buf_len < 0) return EOS;
            }
            int res = inp_buf[inp_buf_pos++] & 0xff;
            if (bin_data_size > 0) {
                bin_data_size--;
                return res;
            }
            if (res != ESC) return res;
            while (inp_buf_pos >= inp_buf_len) {
                inp_buf_len = get(inp_buf);
                inp_buf_pos = 0;
                if (inp_buf_len < 0) return EOS;
            }
            int n = inp_buf[inp_buf_pos++] & 0xff;
            switch (n) {
            case 0: return ESC;
            case 1: return EOM;
            case 2: return EOS;
            case 3:
                for (int i = 0;; i += 7) {
                    while (inp_buf_pos >= inp_buf_len) {
                        inp_buf_len = get(inp_buf);
                        inp_buf_pos = 0;
                        if (inp_buf_len < 0) return EOS;
                    }
                    int m = inp_buf[inp_buf_pos++] & 0xff;
                    bin_data_size |= (m & 0x7f) << i;
                    if ((m & 0x80) == 0) break;
                }
                break;
            default:
                throw new IOException("Invalid escape sequence: " + ESC + " " + n);
            }
        }
    }

    @Override
    protected final void write(int n) throws IOException {
        switch (n) {
        case ESC:
            esc_buf[0] = ESC;
            esc_buf[1] = 0;
            put(esc_buf, 0, 2);
            break;
        case EOM:
            esc_buf[0] = ESC;
            esc_buf[1] = 1;
            put(esc_buf, 0, 2);
            break;
        case EOS:
            esc_buf[0] = ESC;
            esc_buf[1] = 2;
            put(esc_buf, 0, 2);
            break;
        default:
            assert n >= 0 && n <= 0xff;
            put(n);
            break;
        }
    }

    @Override
    protected final void write(byte[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    @Override
    protected final void write(byte[] buf, int pos, int len) throws IOException {
        if (len > 32 && isZeroCopySupported()) {
            int n = len;
            int esc_buf_pos = 0;
            esc_buf[esc_buf_pos++] = ESC;
            esc_buf[esc_buf_pos++] = 3;
            for (;;) {
                if (n <= 0x7f) {
                    esc_buf[esc_buf_pos++] = (byte)n;
                    break;
                }
                esc_buf[esc_buf_pos++] = (byte)((n & 0x7f) | 0x80);
                n = n >> 7;
            }
            put(esc_buf, 0, esc_buf_pos);
            put(buf, pos, len);
        }
        else {
            int esc_buf_pos = 0;
            int end = pos + len;
            for (int i = pos; i < end; i++) {
                if (esc_buf_pos + 2 > esc_buf.length) {
                    put(esc_buf, 0, esc_buf_pos);
                    esc_buf_pos = 0;
                }
                byte b = buf[i];
                esc_buf[esc_buf_pos++] = b;
                if (b == ESC) esc_buf[esc_buf_pos++] = 0;
            }
            put(esc_buf, 0, esc_buf_pos);
        }
    }
}
