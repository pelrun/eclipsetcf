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
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedList;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IStreams;

/**
 * TCFVirtualInputStream is OutputStream implementation over TCF Streams service.
 *
 * @since 1.2
 */
public final class TCFVirtualOutputStream extends OutputStream {

    private static final int MAX_QUEUE = 32;

    private final IChannel channel;
    private final IStreams streams;
    private final String id;
    private final boolean send_eos;
    private final Runnable on_close;
    private final byte[] buf = new byte[1];
    private final HashSet<IToken> queue = new HashSet<IToken>();
    private final LinkedList<Exception> errors = new LinkedList<Exception>();
    private final HashSet<Runnable> wait_list = new HashSet<Runnable>();
    private boolean closed;

    public TCFVirtualOutputStream(IChannel channel, String id, boolean send_eos, Runnable on_close) throws IOException {
        this.channel = channel;
        streams = channel.getRemoteService(IStreams.class);
        if (streams == null) throw new IOException("Streams service not available"); //$NON-NLS-1$
        this.id = id;
        this.send_eos = send_eos;
        this.on_close = on_close;
    }

    @Override
    public synchronized void write(final byte b[], final int off, final int len) throws IOException {
        if (closed) throw new IOException("Stream is closed"); //$NON-NLS-1$
        if (b == null) throw new NullPointerException();
        if (off < 0 || off > b.length || len < 0 || len > b.length - off) throw new IndexOutOfBoundsException();
        if (len == 0) return;
        new TCFTask<Object>() {
            public void run() {
                if (queue.size() > MAX_QUEUE) {
                    wait_list.add(this);
                    return;
                }
                if (errors.size() > 0) {
                    error(errors.removeFirst());
                    return;
                }
                queue.add(streams.write(id, b, off, len, new IStreams.DoneWrite() {
                    public void doneWrite(IToken token, Exception error) {
                        if (error != null) errors.add(error);
                        queue.remove(token);
                        if (wait_list.size() > 0) {
                            Runnable[] list = wait_list.toArray(new Runnable[wait_list.size()]);
                            wait_list.clear();
                            for (Runnable r : list) r.run();
                        }
                    }
                }));
                done(this);
            }
        }.getIO();
    }

    @Override
    public synchronized void write(int b) throws IOException {
        buf[0] = (byte)b;
        write(buf, 0, 1);
    }

    @Override
    public void flush() throws IOException {
        if (closed) throw new IOException("Stream is closed"); //$NON-NLS-1$
        new TCFTask<Object>() {
            public void run() {
                if (queue.size() > 0) {
                    wait_list.add(this);
                }
                else if (errors.size() > 0) {
                    error(errors.removeFirst());
                }
                else {
                    done(this);
                }
            }
        }.getIO();
    }

    @Override
    public void close() throws IOException {
        flush();
        if (closed) return;
        closed = true;
        if (send_eos) {
            new TCFTask<Object>() {
                public void run() {
                    streams.eos(id, new IStreams.DoneEOS() {
                        public void doneEOS(IToken token, Exception error) {
                            if (error != null && channel.getState() != IChannel.STATE_CLOSED) {
                                error(error);
                            }
                            else {
                                done(this);
                            }
                        }
                    });
                }
            }.getIO();
        }
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
    }}
