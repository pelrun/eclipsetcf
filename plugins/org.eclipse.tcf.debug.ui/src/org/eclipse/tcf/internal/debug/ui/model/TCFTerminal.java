package org.eclipse.tcf.internal.debug.ui.model;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IStreams;
import org.eclipse.tcf.util.TCFTask;
import org.eclipse.tcf.util.TCFVirtualOutputStream;
import org.eclipse.tm.internal.terminal.control.ITerminalViewControl;
import org.eclipse.tm.internal.terminal.provisional.api.ISettingsPage;
import org.eclipse.tm.internal.terminal.provisional.api.ISettingsStore;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalControl;
import org.eclipse.tm.internal.terminal.provisional.api.TerminalState;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

@SuppressWarnings("restriction")
class TCFTerminal {

    class Connection implements ITerminalConnector {

        private final String name;
        private final String id;
        private final String rx_id;
        private final String tx_id;

        private TCFVirtualStreamProxy inp;
        private TCFVirtualOutputStream out;
        private ITerminalControl control;
        private boolean connected;

        Connection(String name, String id, String rx_id, String tx_id) {
            this.name = name;
            this.id = id;
            this.rx_id = rx_id;
            this.tx_id = tx_id;
        }

        void dispose() {
            model.asyncExec(new Runnable() {
                @Override
                public void run() {
                    disconnect();
                }
            });
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Object getAdapter(Class adapter) {
            return null;
        }

        @Override
        public void setTerminalSize(int w, int h) {
        }

        @Override
        public void save(ISettingsStore store) {
        }

        @Override
        public ISettingsPage makeSettingsPage() {
            return null;
        }

        @Override
        public void load(ISettingsStore store) {
        }

        @Override
        public boolean isLocalEcho() {
            return false;
        }

        @Override
        public boolean isInitialized() {
            return control != null;
        }

        @Override
        public boolean isHidden() {
            return false;
        }

        @Override
        public OutputStream getTerminalToRemoteStream() {
            return out;
        }

        @Override
        public String getSettingsSummary() {
            return null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getInitializationErrorMessage() {
            return null;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void disconnect() {
            if (!connected) return;
            try {
                inp.close();
                out.close();
            }
            catch (Exception x) {
                Activator.log("Cannot disconnect a terminal", x);
            }
            control.setState(TerminalState.CLOSED);
            connected = false;
        }

        @Override
        public void connect(ITerminalControl control) {
            assert !connected;
            try {
                control.setState(TerminalState.CONNECTING);
                this.control = control;
                control.setEncoding("UTF8");
                final OutputStream rtt = control.getRemoteToTerminalOutputStream();
                new TCFTask<Object>() {
                    int cnt;
                    @Override
                    public void run() {
                        try {
                            if (cnt == 0) {
                                IStreams.DoneConnect done_connect = new IStreams.DoneConnect() {
                                    @Override
                                    public void doneConnect(IToken token, Exception error) {
                                        if (error != null) {
                                            if (!isDone()) error(error);
                                        }
                                        else {
                                            cnt++;
                                            run();
                                        }
                                    }
                                };
                                streams.connect(tx_id, done_connect);
                                streams.connect(rx_id, done_connect);
                            }
                            else if (cnt == 2) {
                                inp = new TCFVirtualStreamProxy(tx_id, rtt);
                                out = new TCFVirtualOutputStream(channel, rx_id, false, null);
                                done(this);
                            }
                        }
                        catch (Exception x) {
                            error(x);
                        }
                    }
                }.get();
                control.setState(TerminalState.CONNECTED);
                connected = true;
            }
            catch (Exception x) {
                Activator.log("Cannot connect a terminal", x);
                control.setState(TerminalState.CLOSED);
            }
        }
    }

    private class TCFVirtualStreamProxy {

        private final String id;
        boolean closed;

        TCFVirtualStreamProxy(final String id, final OutputStream out) {
            this.id = id;
            assert Protocol.isDispatchThread();
            IStreams.DoneRead done = new IStreams.DoneRead() {
                @Override
                public void doneRead(IToken token, Exception error, int lost_size, byte[] data, boolean eos) {
                    if (data != null) {
                        try {
                            out.write(data);
                        }
                        catch (Exception x) {
                            if (!closed) Activator.log("Terminal view error", x);
                        }
                    }
                    if (!closed && !eos && error == null) {
                        streams.read(id, 0x1000, this);
                    }
                }
            };
            streams.read(id, 0x1000, done);
            streams.read(id, 0x1000, done);
            streams.read(id, 0x1000, done);
            streams.read(id, 0x1000, done);
        }

        void close() throws IOException {
            if (closed) return;
            closed = true;
            new TCFTask<Object>() {
                @Override
                public void run() {
                    streams.disconnect(id, new IStreams.DoneDisconnect() {
                        @Override
                        public void doneDisconnect(IToken token, Exception error) {
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
    }

    private final TCFModel model;
    private final IChannel channel;
    private final IStreams streams;

    private final LinkedList<Connection> connections = new LinkedList<Connection>();

    private IViewPart view;
    private static int view_cnt = 0;

    TCFTerminal(TCFModel model) {
        this.model = model;
        this.channel = model.getChannel();
        streams = channel.getRemoteService(IStreams.class);
    }

    void dispose() {
        assert Protocol.isDispatchThread();
        model.asyncExec(new Runnable() {
            @Override
            public void run() {
                for (Connection c : connections) {
                    if (c.control != null) ((ITerminalViewControl)c.control).disposeTerminal();
                    c.control = null;
                    c.inp = null;
                    c.out = null;
                }
                connections.clear();
                if (view != null) {
                    view.getViewSite().getPage().hideView(view);
                    view = null;
                }
            }
        });
    }

    Connection connect(String name, String id, final Map<String,Object> uart) {
        assert Protocol.isDispatchThread();
        final Connection c = new Connection(name, id, (String)uart.get("RXStreamID"), (String)uart.get("TXStreamID"));
        if (streams != null && c.rx_id != null && c.tx_id != null) {
            model.asyncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        // If the (old, legacy, deprecated) terminal view is available, show it.
                        if (PlatformUI.getWorkbench().getViewRegistry().find("org.eclipse.tm.terminal.view.TerminalView") != null) { //$NON-NLS-1$
                            if (view == null) {
                                IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                                if (window == null) return;
                                IWorkbenchPage page = window.getActivePage();
                                if (page == null) return;
                                view = page.showView(
                                        "org.eclipse.tm.terminal.view.TerminalView", //$NON-NLS-1$
                                        "TCFTerminal" + view_cnt, IWorkbenchPage.VIEW_ACTIVATE);
                                view_cnt++;
                            }
                            connections.add(c);
                            
                            // Must use reflection here as we cannot have a hard dependency to "org.eclipse.tm.terminal.view"
                            if (view != null) {
                                Class<?> clazz = view.getClass();
                                if ("ITerminalView".equals(clazz.getSimpleName())) {
                                    try {
                                        Method m = clazz.getMethod("newTerminal", ITerminalConnector.class);
                                        m.invoke(view, c);
                                    } catch (NoSuchMethodException e) {
                                        Activator.log("Cannot open Terminal view", e);
                                    } catch (InvocationTargetException e) {
                                        Activator.log("Cannot open Terminal view", e);
                                    } catch (IllegalAccessException e) {
                                        Activator.log("Cannot open Terminal view", e);
                                    }
                                }
                            }
                        }
                    }
                    catch (PartInitException x) {
                        Activator.log("Cannot open Terminal view", x);
                    }
                }
            });
        }
        return c;
    }
}
