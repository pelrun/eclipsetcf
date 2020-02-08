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
package org.eclipse.tcf.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IStreams;

/**
 * TCFVirtualInputStream is InputStream implementation over TCF Streams service.
 *
 * @since 1.2
 */
public final class TCFVirtualInputStream extends InputStream {

    private static final int MAX_QUEUE = 8;

    private static class Buffer {
        IToken token;
        Exception error;
        byte[] buf;
        int pos;
        boolean eof;
    }

    private final IChannel channel;
    private final IStreams streams;
    private final String id;
    private final Runnable on_close;
    private final LinkedList<Buffer> queue = new LinkedList<Buffer>();
    private Buffer buf;
    private TCFTask<Buffer> task;
    private byte[] tmp = new byte[1];
    private boolean closed;
    private boolean eof;

    public TCFVirtualInputStream(IChannel channel, String id, Runnable on_close) throws IOException {
        this.channel = channel;
        streams = channel.getRemoteService(IStreams.class);
        if (streams == null) throw new IOException("Streams service not available"); //$NON-NLS-1$
        this.id = id;
        this.on_close = on_close;
    }

    @Override
    public synchronized int read(byte b[], final int off, final int len) throws IOException {
        if (closed) throw new IOException("Stream is closed"); //$NON-NLS-1$
        if (b == null) throw new NullPointerException();
        if (off < 0 || off > b.length || len < 0 || len > b.length - off) throw new IndexOutOfBoundsException();
        if (len == 0) return 0;
        try {
            for (;;) {
                if (buf == null) {
                    buf = new TCFTask<Buffer>() {
                        public void run() {
                            while (!eof && queue.size() < MAX_QUEUE) {
                                final Buffer nxt = new Buffer();
                                queue.add(nxt);
                                nxt.token = streams.read(id, 0x10000, new IStreams.DoneRead() {
                                    public void doneRead(IToken token, Exception error, int lost, byte[] data, boolean eos) {
                                        assert nxt.token == token;
                                        nxt.token = null;
                                        nxt.error = error;
                                        nxt.buf = data;
                                        nxt.eof = eos;
                                        if (!eof && (eos || error != null)) eof = true;
                                        if (task != null) {
                                            assert queue.getFirst() == nxt;
                                            task.done(queue.removeFirst());
                                            task = null;
                                        }
                                    }
                                });
                            }
                            if (queue.getFirst().token == null) {
                                done(queue.removeFirst());
                            }
                            else {
                                task = this;
                            }
                        }
                    }.getIO();
                }
                if (buf.buf != null && buf.pos < buf.buf.length) {
                    int n = len;
                    if (n > buf.buf.length - buf.pos) n = buf.buf.length - buf.pos;
                    System.arraycopy(buf.buf, buf.pos, b, off, n);
                    buf.pos += n;
                    return n;
                }
                if (buf.error instanceof IOException) throw (IOException)buf.error;
                if (buf.error != null) throw new IOException(buf.error);
                if (buf.eof) return -1;
                buf = null;
            }
        }
        catch (IOException e) {
            if (closed) return -1;
            throw e;
        }
    }

    @Override
    public synchronized int read() throws IOException {
        if (!closed && buf != null && buf.buf != null && buf.pos < buf.buf.length) {
            return buf.buf[buf.pos++] & 0xff;
        }
        int n = read(tmp, 0, 1);
        if (n < 0) return -1;
        assert n == 1;
        return tmp[0] & 0xff;
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        new TCFTask<Object>() {
            public void run() {
                streams.disconnect(id, new IStreams.DoneDisconnect() {
                    public void doneDisconnect(IToken token, Exception error) {
                        if (error != null && channel.getState() != IChannel.STATE_CLOSED) {
                            error(error);
                        }
                        else {
                            if (on_close != null) on_close.run();
                            done(this);
                        }
                    }
                });
            }
        }.getIO();
    }
}
