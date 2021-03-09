/*******************************************************************************
 * Copyright (c) 2018-2021 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.eclipse.tcf.internal.core.Token;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ILocator;

/**
 * ChannelHTTP implements TCF channel over HTTP protocol.
 * @since 1.7
 */
public class ChannelHTTP extends AbstractChannel {

    private static int id_cnt = 0;
    private final String id = UUID.randomUUID().toString() +
            "-" + Integer.toHexString(id_cnt++);

    private final String host;
    private final int port;

    private boolean stopped;

    private byte[] wr_buf = new byte[0x1000];
    private int wr_cnt;

    public ChannelHTTP(IPeer remote_peer, String host, int port) {
        super(remote_peer);
        this.host = host;
        this.port = port;
        Protocol.invokeLater(new Runnable() {
            @Override
            public void run() {
                start();
            }
        });
    }

    @Override
    public boolean isZeroCopySupported() {
        return false;
    }

    @Override
    protected int read() throws IOException {
        String nm = "http://" + host + ":" + port + "/tcf/sse";
        URL url = new URL(nm);
        while (!stopped) {
            try {
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestProperty("Content-Type", "text/event-stream");
                con.setRequestProperty("X-Session-ID", id);
                con.setRequestMethod("GET");
                BufferedReader inp = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                while (!stopped) {
                    String s = inp.readLine();
                    if (s == null) break;
                    if (s.length() > 0) continue;
                    Protocol.invokeLater(new Runnable() {
                        IToken cmd;
                        @Override
                        public void run() {
                            if (cmd != null) return;
                            if (getState() != STATE_OPEN) return;
                            ILocator l = getRemoteService(ILocator.class);
                            cmd = l.sync(new ILocator.DoneSync() {
                                @Override
                                public void doneSync(IToken token) {
                                    assert cmd == token;
                                    cmd = null;
                                }
                            });
                        }
                    });
                }
                inp.close();
            }
            catch (Throwable x) {
                if (x instanceof FileNotFoundException) throw new IOException("Page not found: " + x.getMessage());
                if (x instanceof IOException) throw (IOException)x;
                throw new IOException(x);
            }
        }
        return -1;
    }

    @Override
    protected void write(int n) throws IOException {
        if (n < 0) {
            if (wr_cnt > 0) {
                try {
                    int i = 0;
                    char type = (char)wr_buf[i++];
                    checkEndOfString(i++);
                    switch (type) {
                    case 'C':
                        sendCommand(i);
                        break;
                    case 'E':
                        sendEvent(i);
                        break;
                    }
                }
                catch (Throwable x) {
                    if (x instanceof FileNotFoundException) throw new IOException("Page not found: " + x.getMessage());
                    if (x instanceof IOException) throw (IOException)x;
                    throw new IOException(x);
                }
                finally {
                    wr_cnt = 0;
                }
            }
            return;
        }
        if (wr_cnt >= wr_buf.length) {
            byte[] t = new byte[wr_cnt * 2];
            System.arraycopy(wr_buf, 0, t, 0, wr_cnt);
            wr_buf = t;
        }
        wr_buf[wr_cnt++] = (byte)n;
    }

    @Override
    protected final void write(byte[] buf) throws IOException {
        write(buf, 0, buf.length);
    }

    @Override
    protected final void write(byte[] buf, int pos, int len) throws IOException {
        if (wr_cnt + len > wr_buf.length) {
            byte[] t = new byte[(wr_cnt + len) * 2];
            System.arraycopy(wr_buf, 0, t, 0, wr_cnt);
            wr_buf = t;
        }
        System.arraycopy(buf, pos, wr_buf, wr_cnt, len);
        wr_cnt += len;
    }

    @Override
    protected void flush() throws IOException {
    }

    @Override
    protected void stop() throws IOException {
        assert !stopped;
        stopped = true;
        URL url = new URL("http://" + host + ":" + port + "/tcf/stop/");
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("X-Session-ID", id);
        con.setRequestMethod("GET");
        con.getInputStream().close();
    }

    private char toHexDigit(int n) {
        if (n >= 0 && n <= 9) return (char)('0' + n);
        if (n >= 10 && n <= 15) return (char)('A' + n - 10);
        return ' ';
    }

    private void checkEndOfString(int i) throws Exception {
        if (i >= wr_cnt || wr_buf[i] != 0) throw new IOException("Invalid message format");
    }

    private String getArgs(int i) throws Exception {
        if (i >= wr_cnt) return null;
        StringBuffer args = new StringBuffer();
        while (i < wr_cnt) {
            if (args.length() > 0) args.append('&');
            while (wr_buf[i] != 0) {
                char ch = (char)(wr_buf[i++] & 0xff);
                if (ch <= ' ' || ch == '%' || ch == '#' || ch == '&' || ch >= (char)127) {
                    args.append('%');
                    args.append(toHexDigit(((int)ch >> 4) & 0xf));
                    args.append(toHexDigit((int)ch & 0xf));
                }
                else {
                    args.append(ch);
                }
            }
            checkEndOfString(i++);
        }
        return args.toString();
    }

    private void sendCommand(int i) throws Exception {
        int p = i;
        while (i < wr_cnt && wr_buf[i] != 0) i++;
        byte[] t = new byte[i - p];
        System.arraycopy(wr_buf, p, t, 0, t.length);
        Token token = new Token(t);
        checkEndOfString(i++);

        p = i;
        while (i < wr_cnt && wr_buf[i] != 0) i++;
        String service = new String(wr_buf, p, i - p, "UTF-8");
        checkEndOfString(i++);

        p = i;
        while (i < wr_cnt && wr_buf[i] != 0) i++;
        String command = new String(wr_buf, p, i - p, "UTF-8");
        checkEndOfString(i++);

        sendRequest(token, service, command, getArgs(i));
    }

    private void sendEvent(int i) throws Exception {
        int p = i;
        while (i < wr_cnt && wr_buf[i] != 0) i++;
        String service = new String(wr_buf, p, i - p, "UTF-8");
        checkEndOfString(i++);

        p = i;
        while (i < wr_cnt && wr_buf[i] != 0) i++;
        String command = new String(wr_buf, p, i - p, "UTF-8");
        checkEndOfString(i++);

        sendRequest(null, service, command, getArgs(i));
    }

    @SuppressWarnings("unchecked")
    private void sendRequest(final Token token, String service, String command, String args) throws Exception {
        String nm = token != null ? "/tcf/c/" + token + "/" : "/tcf/e/";
        nm = "http://" + host + ":" + port + nm + service + "/" + command;
        if (args != null && args.length() > 0) nm += "?" + args;
        assert !stopped;
        URL url = new URL(nm);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("X-Session-ID", id);
        con.setRequestMethod("GET");
        InputStream inp = con.getInputStream();
        final byte[] buf = new byte[con.getHeaderFieldInt("Content-Length", 0)];
        int pos = 0;
        while (pos < buf.length) {
            int rd = inp.read(buf, pos, buf.length - pos);
            if (rd < 0) break;
            pos += rd;
        }
        while (inp.read() > 0) {}
        inp.close();
        Protocol.invokeLater(new Runnable() {
            public void run() {
                try {
                    Object obj = JSON.parseOne(buf);
                    if (obj instanceof Collection) {
                        for (Object x : (Collection<Object>)obj) handleReply(x);
                    }
                    else {
                        throw new Exception("Invalid HTTP reply");
                    }
                }
                catch (Exception x) {
                    Protocol.log("Cannot execute HTTP request", x);
                }
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private void handleReply(Object obj) throws Exception {
        if (obj instanceof Map) {
            Map m = (Map)obj;
            String error = (String)m.get("Error");
            if (error != null) throw new Exception(error);
            String type = (String)m.get("Type");
            final Message msg = new Message(type.charAt(0));
            switch (msg.type) {
            case 'C':
                msg.token = new Token(((String)m.get("Token")).getBytes("UTF-8"));
                msg.service = (String)m.get("Service");
                msg.name = (String)m.get("Command");
                msg.data = readArgs(m.get("Args"));
                break;
            case 'P':
            case 'R':
            case 'N':
                msg.token = new Token(((String)m.get("Token")).getBytes("UTF-8"));
                msg.data = readArgs(m.get("Args"));
                break;
            case 'E':
                msg.service = (String)m.get("Service");
                msg.name = (String)m.get("Event");
                msg.data = readArgs(m.get("Args"));
                if (msg.service.equals(ILocator.NAME) &&
                        msg.name.equals("Hello") &&
                        getState() != STATE_OPENING) return;
                break;
            case 'F':
                msg.data = readArgs(m.get("Args"));
                break;
            default:
                throw new Exception("Invalid HTTP reply");
            }
            handleInput(msg);
        }
        else {
            throw new Exception("Invalid HTTP reply");
        }
    }

    @SuppressWarnings("rawtypes")
    private byte[] readArgs(Object obj) throws Exception {
        byte[] res = null;
        if (obj instanceof Collection) {
            res = JSON.toJSONSequence(((Collection)obj).toArray());
        }
        else if (obj != null) {
            throw new Exception("Invalid HTTP reply");
        }
        return res;
    }
}
