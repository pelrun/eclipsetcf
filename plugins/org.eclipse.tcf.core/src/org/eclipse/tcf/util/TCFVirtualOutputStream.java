/*******************************************************************************
 * Copyright (c) 2013 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.util;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IStreams;

/**
 * TCFVirtualInputStream is OutputStream implementation over TCF Streams service.
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public final class TCFVirtualOutputStream extends OutputStream {

    private final IStreams streams;
    private final String id;
    private final Runnable on_close;
    private final byte[] buf = new byte[1];
    private boolean closed;

    public TCFVirtualOutputStream(IChannel channel, String id, Runnable on_close) throws IOException{
        streams = channel.getRemoteService(IStreams.class);
        if (streams == null) throw new IOException("Streams service not available"); //$NON-NLS-1$
        this.id = id;
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
                streams.write(id, b, off, len, new IStreams.DoneWrite() {
                    public void doneWrite(IToken token, Exception error) {
                        if (error != null) error(error);
                        else done(this);
                    }
                });
            }
        }.getIO();
    }

    @Override
    public synchronized void write(int b) throws IOException {
        buf[0] = (byte)b;
        write(buf, 0, 1);
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        new TCFTask<Object>() {
            public void run() {
                streams.eos(id, new IStreams.DoneEOS() {
                    public void doneEOS(IToken token, Exception error) {
                        if (error != null) {
                            error(error);
                            return;
                        }
                        streams.disconnect(id, new IStreams.DoneDisconnect() {
                            public void doneDisconnect(IToken token, Exception error) {
                                if (error != null) {
                                    error(error);
                                    return;
                                }
                                if (on_close != null) on_close.run();
                                done(this);
                            }
                        });
                    }
                });
            }
        }.getIO();
    }}
