/*******************************************************************************
 * Copyright (c) 2017 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.debug.gdb.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import org.eclipse.tcf.core.TransientPeer;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IEventQueue;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IDiagnostics;
import org.eclipse.tcf.services.IRunControl;

public class Main {

    private static class EventQueue extends Thread implements IEventQueue {

        private final LinkedList<Runnable> queue = new LinkedList<Runnable>();

        EventQueue() {
            setName("TCF Event Dispatcher");
            start();
        }

        public void run() {
            try {
                while (true) {
                    Runnable r = null;
                    synchronized (this) {
                        while (queue.size() == 0) wait();
                        r = queue.removeFirst();
                    }
                    r.run();
                }
            }
            catch (Throwable x) {
                x.printStackTrace();
                System.exit(1);
            }
        }

        public synchronized int getCongestion() {
            int n = queue.size() - 100;
            if (n > 100) n = 100;
            return n;
        }

        public synchronized void invokeLater(Runnable runnable) {
            queue.add(runnable);
            notify();
        }

        public boolean isDispatchThread() {
            return Thread.currentThread() == this;
        }
    }

    private static class RemotePeer extends TransientPeer {

        private final ArrayList<Map<String,String>> attrs;

        public RemotePeer(ArrayList<Map<String,String>> attrs) {
            super(attrs.get(0));
            this.attrs = attrs;
        }

        public IChannel openChannel() {
            assert Protocol.isDispatchThread();
            IChannel c = super.openChannel();
            for (int i = 1; i < attrs.size(); i++) c.redirect(attrs.get(i));
            return c;
        }
    }

    private static class Test implements Runnable {

        final Random rnd = new Random();

        final IRunControl srv_rc;
        final IDiagnostics srv_diag;
        final Map<String,IRunControl.RunControlContext> map_ctx = new HashMap<String,IRunControl.RunControlContext>();

        String test_id;
        String test_ctx_id;
        Thread test_thread;

        Test(IChannel channel) {
            srv_rc = channel.getRemoteService(IRunControl.class);
            srv_diag = channel.getRemoteService(IDiagnostics.class);
            srv_rc.addListener(new IRunControl.RunControlListener() {
                @Override
                public void contextSuspended(String context, String pc, String reason, Map<String, Object> params) {
                }
                @Override
                public void contextResumed(String context) {
                }
                @Override
                public void contextRemoved(String[] context_ids) {
                    for (String id : context_ids) {
                        map_ctx.remove(id);
                    }
                }
                @Override
                public void contextException(String context, String msg) {
                }
                @Override
                public void contextChanged(IRunControl.RunControlContext[] contexts) {
                    for (IRunControl.RunControlContext ctx : contexts) {
                        map_ctx.put(ctx.getID(), ctx);
                    }
                }
                @Override
                public void contextAdded(IRunControl.RunControlContext[] contexts) {
                    for (IRunControl.RunControlContext ctx : contexts) {
                        map_ctx.put(ctx.getID(), ctx);
                    }
                }
                @Override
                public void containerSuspended(String context, String pc, String reason, Map<String, Object> params, String[] suspended_ids) {
                }
                @Override
                public void containerResumed(String[] context_ids) {
                }
            });
        }

        @Override
        public void run() {
            if (test_id == null) {
                srv_diag.getTestList(new IDiagnostics.DoneGetTestList() {
                    public void doneGetTestList(IToken token, Throwable error, String[] list) {
                        if (error != null) {
                            exit(error);
                        }
                        else {
                            if (list.length == 0) {
                                exit(new Exception("Target does not support RCBP tests"));
                            }
                            test_id = list[rnd.nextInt(list.length)];
                            run();
                        }
                    }
                });
                return;
            }
            if (test_ctx_id == null) {
                srv_diag.runTest(test_id, new IDiagnostics.DoneRunTest() {
                    public void doneRunTest(IToken token, Throwable error, String id) {
                        if (error != null) {
                            exit(error);
                        }
                        else if (id == null) {
                            exit(new Exception("Test context ID must not be null"));
                        }
                        else if (map_ctx.get(id) == null) {
                            exit(new Exception("Missing context added event"));
                        }
                        else {
                            test_ctx_id = id;
                            run();
                        }
                    }
                });
                return;
            }
            if (test_thread == null) {
                new TestThread().start();
                return;
            }
            exit(new Exception("Internal error"));
        }

        void exit(Throwable error) {
            if (error != null) {
                error.printStackTrace();
                System.exit(6);
            }
            System.exit(0);
        }
    }

    private static IPeer getPeer(String[] arr) {
        ArrayList<Map<String,String>> l = new ArrayList<Map<String,String>>();
        for (String s : arr) {
            Map<String,String> map = new HashMap<String,String>();
            int len = s.length();
            int i = 0;
            while (i < len) {
                int i0 = i;
                while (i < len && s.charAt(i) != '=' && s.charAt(i) != 0) i++;
                int i1 = i;
                if (i < len && s.charAt(i) == '=') i++;
                int i2 = i;
                while (i < len && s.charAt(i) != ':') i++;
                int i3 = i;
                if (i < len && s.charAt(i) == ':') i++;
                String key = s.substring(i0, i1);
                String val = s.substring(i2, i3);
                map.put(key, val);
                if (key.equals("Host")) TestThread.host = val;
            }
            l.add(map);
        }
        return new RemotePeer(l);
    }

    private static void runTestSuite(IPeer peer) {
        final IChannel channel = peer.openChannel();
        channel.addChannelListener(new IChannel.IChannelListener() {
            public void congestionLevel(int level) {
            }
            public void onChannelClosed(final Throwable error) {
                if (error != null) {
                    error.printStackTrace();
                    System.exit(6);
                }
            }
            public void onChannelOpened() {
                Protocol.invokeLater(new Test(channel));
            }
        });
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Missing command line argument - peer identification string");
            System.exit(4);
        }
        Protocol.setEventQueue(new EventQueue());
        Protocol.invokeLater(new Runnable() {
            public void run() {
                runTestSuite(getPeer(args));
            }
        });
        String to_env = System.getenv().get("TCF_TEST_TIMEOUT");
        if (to_env == null) to_env = "10";
        final long to_min = Long.parseLong(to_env);
        Protocol.invokeLater(to_min * 60 * 1000, new Runnable() {
            public void run() {
                System.err.println("Error: timeout - test has not finished in " + to_min + " min");
                System.exit(5);
            }
        });
    }
}
