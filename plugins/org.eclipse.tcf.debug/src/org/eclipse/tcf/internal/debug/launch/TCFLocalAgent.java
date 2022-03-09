/*******************************************************************************
 * Copyright (c) 2009-2022 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.launch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.internal.debug.Activator;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ILocator;
import org.eclipse.tcf.util.TCFTask;
import org.osgi.framework.Bundle;

/**
 * This class checks that TCF Agent is running on the local host,
 * and starts a new instance of the agent if it cannot be located.
 */
public class TCFLocalAgent {

    public static final String
        LOCAL_HOST = "127.0.0.1",
        AGENT_NAME = "agent",
        SERVER_NAME = "server";

    private static final Map<String,String> ports = new HashMap<String,String>();
    private static final Map<String,Process> agents = new HashMap<String,Process>();

    static {
        ports.put(AGENT_NAME, "1534");
        ports.put(SERVER_NAME, "1535");
    }

    private static boolean destroed;

    private static String getSysName() {
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        if (arch.equals("x86")) arch = "i386";
        if (arch.equals("i686")) arch = "i386";
        if (os.startsWith("Windows")) os = "Windows";
        if (os.equals("Linux")) os = "GNU/Linux";
        return os + "/" + arch;
    }

    private static Path getDevelAgentFileName(String nm) {
        try {
            String fnm = nm;
            String sys = getSysName();
            if (sys.startsWith("Windows")) {
                sys = "MSVC/Win32";
                fnm += ".exe";
            }
            Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
            File plugin = FileLocator.getBundleFile(bundle);
            File agent = new File(plugin, "../../../org.eclipse.tcf.agent/" + nm + "/obj/" + sys + "/Debug/" + fnm);
            if (!agent.exists()) return null;
            return new Path(agent.getAbsolutePath());
        }
        catch (Exception x) {
            Activator.log("Cannot find bundle location", x);
        }
        return null;
    }

    private static Path getAgentFileName(String fnm) {
        String sys = getSysName();
        if (sys.startsWith("Windows")) fnm += ".exe";
        return new Path("agent/" + sys + "/" + fnm);
    }

    public static synchronized String runLocalAgent(final String nm) throws CoreException {
        if (destroed) return null;
        String id = getLocalAgentID(nm);
        if (id != null) return id;
        if (agents.containsKey(nm)) {
            agents.remove(nm).destroy();
        }
        Path fnm = getDevelAgentFileName(nm);
        if (fnm == null) fnm = getAgentFileName(nm);
        try {
            if (!fnm.isAbsolute()) {
                Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
                URL url = FileLocator.find(bundle, fnm, null);
                if (url != null) {
                    URLConnection ucn = url.openConnection();
                    ucn.setRequestProperty("Method", "HEAD");
                    ucn.connect();
                    long mtime = ucn.getLastModified();
                    File f = Activator.getDefault().getStateLocation().append(fnm).toFile();
                    if (!f.exists() || mtime != f.lastModified()) {
                        f.getParentFile().mkdirs();
                        InputStream inp = url.openStream();
                        OutputStream out = new FileOutputStream(f);
                        byte[] buf = new byte[0x1000];
                        for (;;) {
                            int len = inp.read(buf);
                            if (len < 0) break;
                            out.write(buf, 0, len);
                        }
                        out.close();
                        inp.close();
                        if (!"exe".equals(fnm.getFileExtension())) {
                            String[] cmd = {
                                    "chmod",
                                    "a+x",
                                    f.getAbsolutePath()
                            };
                            Runtime.getRuntime().exec(cmd).waitFor();
                        }
                        f.setLastModified(mtime);
                        fnm = new Path(f.getAbsolutePath());
                    }
                }
            }
            String[] cmd = {
                    fnm.toOSString(),
                    "-s",
                    "TCP:" + LOCAL_HOST + ":" + ports.get(nm)
            };
            final Process prs = Runtime.getRuntime().exec(cmd);
            agents.put(nm, prs);
            final TCFTask<String> waiting = waitAgentReady(nm);
            Thread t = new Thread() {
                public void run() {
                    try {
                        final int n = prs.waitFor();
                        final StringBuffer sbf = new StringBuffer();
                        if (n != 0) {
                            char cbf[] = new char[256];
                            InputStreamReader r = new InputStreamReader(prs.getErrorStream());
                            for (;;) {
                                try {
                                    int rd = r.read(cbf);
                                    if (rd < 0) break;
                                    sbf.append(cbf, 0, rd);
                                }
                                catch (IOException x) {
                                    break;
                                }
                            }
                            try {
                                r.close();
                            }
                            catch (IOException x) {
                            }
                            sbf.append("TCF " + nm + " exited with code ");
                            sbf.append(n);
                            Protocol.invokeLater(new Runnable() {
                                public void run() {
                                    if (waiting.isDone()) return;
                                    waiting.error(new IOException(sbf.toString()));
                                }
                            });
                        }
                        synchronized (TCFLocalAgent.class) {
                            if (agents.get(nm) == prs) {
                                if (n != 0 && !destroed) {
                                    Activator.log(sbf.toString(), null);
                                }
                                agents.remove(nm);
                            }
                        }
                    }
                    catch (InterruptedException x) {
                        Activator.log("TCF " + nm + " monitor interrupted", x);
                    }
                }
            };
            t.setDaemon(true);
            t.setName("TCF Agent Monitor");
            t.start();
            return waiting.getIO();
        }
        catch (Throwable x) {
            agents.remove(nm);
            throw new CoreException(new Status(IStatus.ERROR,
                    Activator.PLUGIN_ID, 0,
                    "Cannot start local TCF " + nm + ".",
                    x));
        }
    }

    private static boolean isLocalAgent(IPeer p, String nm) {
        String prot = p.getTransportName();
        if (prot.equals("PIPE")) return true;
        if (prot.equals("UNIX")) {
            String port = p.getAttributes().get(IPeer.ATTR_IP_PORT);
            return ports.get(nm).equals(port);
        }
        String host = p.getAttributes().get(IPeer.ATTR_IP_HOST);
        String port = p.getAttributes().get(IPeer.ATTR_IP_PORT);
        return LOCAL_HOST.equals(host) && ports.get(nm).equals(port);
    }

    public static synchronized String getLocalAgentID(final String nm) {
        return new TCFTask<String>() {
            int cnt;
            public void run() {
                final ILocator locator = Protocol.getLocator();
                for (IPeer p : locator.getPeers().values()) {
                    if (isLocalAgent(p, nm)) {
                        done(p.getID());
                        return;
                    }
                }
                if (cnt++ < 10) {
                    Protocol.invokeLater(100, this);
                }
                else {
                    done(null);
                }
            }
        }.getE();
    }

    private static TCFTask<String> waitAgentReady(final String nm) {
        return new TCFTask<String>() {
            public void run() {
                final ILocator locator = Protocol.getLocator();
                for (IPeer p : locator.getPeers().values()) {
                    if (isLocalAgent(p, nm)) {
                        done(p.getID());
                        return;
                    }
                }
                final ILocator.LocatorListener listener = new ILocator.LocatorListener() {
                    public void peerAdded(IPeer p) {
                        if (!isDone() && isLocalAgent(p, nm)) {
                            done(p.getID());
                            locator.removeListener(this);
                        }
                    }
                    public void peerChanged(IPeer peer) {
                    }
                    public void peerHeartBeat(String id) {
                    }
                    public void peerRemoved(String id) {
                    }
                };
                locator.addListener(listener);
                Protocol.invokeLater(30000, new Runnable() {
                    public void run() {
                        if (!isDone()) {
                            error(new Exception("Timeout waiting for TCF Agent to start"));
                            locator.removeListener(listener);
                        }
                    }
                });
            }
        };
    }

    public static synchronized void destroy() {
        destroed = true;
        for (Process prs : agents.values()) prs.destroy();
        agents.clear();
    }
}
