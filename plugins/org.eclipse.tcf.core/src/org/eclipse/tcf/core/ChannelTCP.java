/*******************************************************************************
 * Copyright (c) 2007, 2017 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;

/**
 * ChannelTCP is a IChannel implementation that works on top of TCP sockets as a transport.
 */
public class ChannelTCP extends StreamChannel {

    private volatile Socket socket;
    private int timeout;
    private InputStream inp;
    private OutputStream out;
    private boolean started;
    private boolean closed;

    private static SSLContext ssl_context;

    public static void setSSLContext(SSLContext ssl_context) {
        ChannelTCP.ssl_context = ssl_context;
    }

    /**
     * Main constructor of ChannelTCP
     * @param remote_peer Remote Peer to which we want to connect
     * @param host Hostname or IP Address of the Remote Peer
     * @param port Port of the Remote Peer
     * @param ssl true if the socket needs to be an SSL socket, false otherwise
     */
    public ChannelTCP(IPeer remote_peer, final String host, final int port, final boolean ssl) {
        super(remote_peer);
        socket = new Socket();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            socket.connect(new InetSocketAddress(host, port), timeout);
                            socket.setTcpNoDelay(true);
                            socket.setKeepAlive(true);
                            if (ssl) {
                                if (ssl_context == null) throw new Exception("SSL context is not set");
                                socket = ssl_context.getSocketFactory().createSocket(socket, host, port, true);
                                ((SSLSocket)socket).startHandshake();
                            }
                            inp = new BufferedInputStream(socket.getInputStream());
                            out = new BufferedOutputStream(socket.getOutputStream());
                            onSocketConnected(null);
                        }
                        catch (Exception x) {
                            onSocketConnected(x);
                        }
                    }
                };
                thread.setName("TCF Socket Connect");
                thread.start();
            }
        });
    }

    /**
     * Constructs a non-secured ChannelTCP, i.e: the socket underlying the ChannelTCP is non-SSL socket
     * @param remote_peer Remote Peer to which we want to connect
     * @param host Hostname or IP Address of the Remote Peer
     * @param port Port of the Remote Peer
     */
    public ChannelTCP(IPeer remote_peer, String host, int port) {
        this(remote_peer, host, port, false);
    }

    /**
     * Construct a non-secured ChannelTCP, i.e: the socket underlying the ChannelTCP is non-SSL socket.
     * @param local_peer local peer
     * @param remote_peer Remote Peer to which we want to connect
     * @param socket socket for the underlying tcp connection
     * @throws IOException
     */
    public ChannelTCP(IPeer local_peer, IPeer remote_peer, Socket socket) throws IOException {
        super(local_peer, remote_peer);
        this.socket = socket;
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        inp = new BufferedInputStream(socket.getInputStream());
        out = new BufferedOutputStream(socket.getOutputStream());
        onSocketConnected(null);
    }

    public void setConnectTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setReuseAddress(boolean on) throws SocketException {
        socket.setReuseAddress(on);
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        socket.setReceiveBufferSize(size);
    }

    public void setSendBufferSize(int size) throws SocketException {
        socket.setSendBufferSize(size);
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        socket.setSoLinger(on, linger);
    }

    public void setTrafficClass(int tc) throws SocketException {
        socket.setTrafficClass(tc);
    }

    public void setPerformancePreferences(int connection_time, int latency, int bandwidth) {
        socket.setPerformancePreferences(connection_time, latency, bandwidth);
    }

    /**
     * @param x exception object, or null if there was no exception creating the ChannelTCP object
     */
    private void onSocketConnected(final Throwable x) {
        Protocol.invokeLater(new Runnable() {
            public void run() {
                if (x != null) {
                    terminate(x);
                    closed = true;
                }
                if (closed) {
                    try {
                        if (socket != null) {
                            socket.close();
                            if (out != null) out.close();
                            if (inp != null) inp.close();
                        }
                    }
                    catch (IOException y) {
                        Protocol.log("Cannot close socket", y);
                    }
                }
                else {
                    started = true;
                    start();
                }
            }
        });
    }

    @Override
    protected final int get() throws IOException {
        try {
            if (closed) return -1;
            return inp.read();
        }
        catch (IOException x) {
            if (closed) return -1;
            throw x;
        }
    }

    @Override
    protected final int get(byte[] buf) throws IOException {
        try {
            if (closed) return -1;
            return inp.read(buf);
        }
        catch (IOException x) {
            if (closed) return -1;
            throw x;
        }
    }

    @Override
    protected final void put(int b) throws IOException {
        assert b >= 0 && b <= 0xff;
        if (closed) return;
        out.write(b);
    }

    @Override
    protected final void put(byte[] buf) throws IOException {
        if (closed) return;
        out.write(buf);
    }

    @Override
    protected final void put(byte[] buf, int pos, int len) throws IOException {
        if (closed) return;
        out.write(buf, pos, len);
    }

    @Override
    protected final void flush() throws IOException {
        if (closed) return;
        out.flush();
    }

    @Override
    protected void stop() throws IOException {
        closed = true;
        if (started) {
            socket.close();
            /*
             * We should not write anything to the stream here, just close it.
             * So, don't call out.close(), because it calls out.flush().
             * The socket output stream is already closed by socket.close().
             */
            inp.close();
        }
    }
}
