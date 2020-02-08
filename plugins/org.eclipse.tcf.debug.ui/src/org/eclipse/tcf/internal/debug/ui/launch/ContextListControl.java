/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.launch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.tcf.internal.debug.ui.ImageCache;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IChannel.IChannelListener;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.services.IProcesses.ProcessContext;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.services.IRunControl.RunControlContext;
import org.eclipse.tcf.services.ISysMonitor;
import org.eclipse.tcf.services.ISysMonitor.SysMonitorContext;
import org.eclipse.tcf.util.TCFTask;

public class ContextListControl {

    private final class ProcessListener implements IProcesses.ProcessesListener {
        public void exited(final String process_id, int exit_code) {
            onContextRemoved(process_id);
        }
    }

    private final class ContextListener implements IRunControl.RunControlListener {
        @Override
        public void contextAdded(RunControlContext[] contexts) {
            for (RunControlContext ctx : contexts) {
                onContextAdded(ctx.getParentID());
            }
        }
        @Override
        public void contextChanged(RunControlContext[] contexts) {
        }
        @Override
        public void contextRemoved(String[] context_ids) {
            for (String id : context_ids) {
                onContextRemoved(id);
            }
        }
        @Override
        public void contextSuspended(String context, String pc, String reason, Map<String, Object> params) {
        }
        @Override
        public void contextResumed(String context) {
        }
        @Override
        public void containerSuspended(String context, String pc, String reason, Map<String, Object> params, String[] suspended_ids) {
        }
        @Override
        public void containerResumed(String[] context_ids) {
        }
        @Override
        public void contextException(String context, String msg) {
        }
    }

    static class ContextInfo {
        String name;
        String id;
        Object additional_info;
        boolean is_attached;
        boolean is_container;
        boolean has_state;
        long pid;
        String[] cmd_line;
        ContextInfo[] children;
        Throwable children_error;
        boolean children_pending;
        boolean children_reload;
        ContextInfo parent;
    }

    private Composite composite;
    private Tree ctx_tree;
    private Display display;
    private IChannel channel;
    private String channel_state;
    private final ContextInfo root_info = new ContextInfo();
    private final ProcessListener prs_listener = new ProcessListener();
    private final ContextListener ctx_listener = new ContextListener();
    private final boolean processes;
    private String initial_selection;

    public ContextListControl(Composite parent, boolean processes) {
        this.processes = processes;
        display = parent.getDisplay();
        parent.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                Protocol.invokeAndWait(new Runnable() {
                    public void run() {
                        disconnectPeer();
                        display = null;
                    }
                });
            }
        });
        createContextListArea(parent);
    }

    public void setInput(final IPeer peer) {
        assert Thread.currentThread() == display.getThread();
        channel = new TCFTask<IChannel>() {
            public void run() {
                disconnectPeer();
                done(connectPeer(peer));
            }
        }.getE();
        root_info.children = null;
        root_info.children_error = null;
        root_info.children_pending = false;
        root_info.children_reload = false;
        loadChildren(root_info);
    }

    public Control getControl() {
        return composite;
    }

    public Tree getTree() {
        return ctx_tree;
    }

    public ContextInfo getSelection() {
        if (ctx_tree != null) {
            initial_selection = null;
            TreeItem[] items = ctx_tree.getSelection();
            if (items.length > 0) {
                ContextInfo info = findInfo(items[0]);
                return info;
            }
        }
        return null;
    }

    private void createContextListArea(Composite parent) {
        Font font = parent.getFont();
        composite = new Composite(parent, SWT.NONE);
        composite.setFont(font);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        ctx_tree = new Tree(composite, SWT.VIRTUAL | SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.minimumHeight = 150;
        gd.minimumWidth = 470;
        ctx_tree.setLayoutData(gd);
        ctx_tree.setFont(font);
        ctx_tree.addListener(SWT.SetData, new Listener() {
            public void handleEvent(Event event) {
                TreeItem item = (TreeItem)event.item;
                ContextInfo info = findInfo(item);
                if (info == null) {
                    updateItems(item.getParentItem(), false);
                }
                else {
                    fillItem(item, info);
                }
            }
        });
    }

    protected void disconnectPeer() {
        if (channel != null && channel.getState() != IChannel.STATE_CLOSED) {
            channel.close();
        }
    }

    protected IChannel connectPeer(IPeer peer) {
        if (peer == null) return null;
        final IChannel channel = peer.openChannel();
        channel.addChannelListener(new IChannelListener() {
            public void congestionLevel(int level) {
            }
            public void onChannelClosed(final Throwable error) {
                if (display != null) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            if (ContextListControl.this.channel != channel) return;
                            root_info.children_error = error;
                            loadChildren(root_info);
                        }
                    });
                }
            }
            public void onChannelOpened() {
                if (processes) {
                    IProcesses service = channel.getRemoteService(IProcesses.class);
                    if (service != null) service.addListener(prs_listener);
                }
                else {
                    IRunControl service = channel.getRemoteService(IRunControl.class);
                    if (service != null) service.addListener(ctx_listener);
                }
                if (display != null) {
                    display.asyncExec(new Runnable() {
                        public void run() {
                            if (ContextListControl.this.channel != channel) return;
                            loadChildren(root_info);
                            updateItems(root_info);
                        }
                    });
                }
            }
        });
        return channel;
    }

    private void updateItems(TreeItem parent_item, boolean reload) {
        final ContextInfo parent_info = findInfo(parent_item);
        if (parent_info == null) {
            parent_item.setText("Invalid");
        }
        else {
            if (reload && parent_info.children_error != null) {
                loadChildren(parent_info);
            }
            display.asyncExec(new Runnable() {
                public void run() {
                    updateItems(parent_info);
                }
            });
        }
    }

    private void updateItems(final ContextInfo parent) {
        if (display == null) return;
        assert Thread.currentThread() == display.getThread();
        TreeItem[] items = null;
        boolean expanded = true;
        if (parent.children == null || parent.children_error != null) {
            if (parent == root_info) {
                ctx_tree.deselectAll();
                ctx_tree.setItemCount(1);
                items = ctx_tree.getItems();
            }
            else {
                TreeItem item = findItem(parent);
                if (item == null) return;
                expanded = item.getExpanded();
                item.setItemCount(1);
                items = item.getItems();
            }
            assert items.length == 1;
            items[0].removeAll();
            items[0].setImage((Image)null);
            items[0].setForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
            if (parent.children_pending) {
                items[0].setText("Pending...");
            }
            else if (parent.children_error != null) {
                String msg = TCFModel.getErrorMessage(parent.children_error, false);
                items[0].setForeground(display.getSystemColor(SWT.COLOR_RED));
                items[0].setText(msg);
            }
            else if (channel_state != null) {
                items[0].setText(channel_state);
            }
            else if (expanded) {
                loadChildren(parent);
                items[0].setText("Pending...");
            }
            else {
                items[0].setText("");
            }
        }
        else {
            ContextInfo[] arr = parent.children;
            if (parent == root_info) {
                ctx_tree.setItemCount(arr.length);
                items = ctx_tree.getItems();
            }
            else {
                TreeItem item = findItem(parent);
                if (item == null) return;
                expanded = item.getExpanded();
                item.setItemCount(expanded ? arr.length : 1);
                items = item.getItems();
            }
            if (expanded) {
                assert items.length == arr.length;
                for (int i = 0; i < items.length; i++) fillItem(items[i], arr[i]);
                // auto-expand single children
                if (items.length == 1 && !items[0].getExpanded()) {
                    items[0].setExpanded(true);
                }
            }
            else {
                items[0].setText("");
            }
        }
        if (initial_selection != null) {
            setInitialSelection(initial_selection);
        }
    }

    private void loadChildren(final ContextInfo parent) {
        assert Thread.currentThread() == display.getThread();
        final IChannel channel = this.channel;
        parent.children_reload = true;
        if (parent.children_pending) return;
        parent.children_reload = false;
        parent.children_pending = true;
        Protocol.invokeLater(new Runnable() {
            public void run() {
                if (channel == null || channel.getState() != IChannel.STATE_OPEN) {
                    doneLoadChildren(channel, parent, null, new ContextInfo[0]);
                }
                else if (processes) {
                    loadProcessChildren(channel, parent);
                }
                else {
                    loadContextChildren(channel, parent);
                }
            }
        });
    }

    private void loadProcessChildren(final IChannel channel, final ContextInfo parent) {
        final IProcesses service = channel.getRemoteService(IProcesses.class);
        final ISysMonitor sysmon = channel.getRemoteService(ISysMonitor.class);
        if (service == null) {
            doneLoadChildren(channel, parent, new Exception("Peer does not support Processes service"), null);
        }
        else if (!canHaveChildren(parent)) {
            doneLoadChildren(channel, parent, null, new ContextInfo[0]);
        }
        else {
            service.getChildren(parent.id, false, new IProcesses.DoneGetChildren() {
                public void doneGetChildren(IToken token, Exception error, String[] context_ids) {
                    if (error != null) {
                        doneLoadChildren(channel, parent, error, null);
                    }
                    else if (context_ids == null || context_ids.length == 0) {
                        doneLoadChildren(channel, parent, null, new ContextInfo[0]);
                    }
                    else {
                        final List<ContextInfo> infos = new ArrayList<ContextInfo>(context_ids.length);
                        final Set<IToken> pending = new HashSet<IToken>();
                        for (final String id : context_ids) {
                            final ContextInfo info = new ContextInfo();
                            info.parent = parent;
                            info.id = id;
                            pending.add(service.getContext(id, new IProcesses.DoneGetContext() {
                                public void doneGetContext(IToken token, Exception error, ProcessContext context) {
                                    if (context != null) {
                                        info.name = context.getName();
                                        info.is_attached = context.isAttached();
                                        infos.add(info);
                                    }
                                    pending.remove(token);
                                    if (!pending.isEmpty()) return;
                                    doneLoadChildren(channel, parent, null, infos.toArray(new ContextInfo[infos.size()]));
                                }
                            }));
                            if (sysmon != null) {
                                pending.add(sysmon.getContext(id, new ISysMonitor.DoneGetContext() {
                                    @Override
                                    public void doneGetContext(IToken token, Exception error, SysMonitorContext context) {
                                        if (context != null) {
                                            info.pid = context.getPID();
                                        }
                                        pending.remove(token);
                                        if (!pending.isEmpty()) return;
                                        doneLoadChildren(channel, parent, null, infos.toArray(new ContextInfo[infos.size()]));
                                    }
                                }));
                                pending.add(sysmon.getCommandLine(id, new ISysMonitor.DoneGetCommandLine() {
                                    @Override
                                    public void doneGetCommandLine(IToken token, Exception error, String[] cmd_line) {
                                        if (cmd_line != null) {
                                            info.cmd_line = cmd_line;
                                        }
                                        pending.remove(token);
                                        if (!pending.isEmpty()) return;
                                        doneLoadChildren(channel, parent, null, infos.toArray(new ContextInfo[infos.size()]));
                                    }
                                }));
                            }
                        }
                    }
                }
            });
        }
    }

    private void loadContextChildren(final IChannel channel, final ContextInfo parent) {
        final IRunControl service = channel.getRemoteService(IRunControl.class);
        if (service == null) {
            doneLoadChildren(channel, parent, new Exception("Peer does not support Run Control service"), null);
        }
        else if (!canHaveChildren(parent)) {
            doneLoadChildren(channel, parent, null, new ContextInfo[0]);
        }
        else {
            service.getChildren(parent.id, new IRunControl.DoneGetChildren() {
                public void doneGetChildren(IToken token, Exception error, String[] context_ids) {
                    if (error != null) {
                        doneLoadChildren(channel, parent, error, null);
                    }
                    else if (context_ids != null && context_ids.length > 0) {
                        final List<ContextInfo> infos = new ArrayList<ContextInfo>(context_ids.length);
                        final Set<IToken> pending = new HashSet<IToken>();
                        for (String id : context_ids) {
                            pending.add(service.getContext(id, new IRunControl.DoneGetContext() {
                                public void doneGetContext(IToken token, Exception error, RunControlContext context) {
                                    if (context != null) {
                                        ContextInfo info = new ContextInfo();
                                        info.parent = parent;
                                        info.id = context.getID();
                                        info.name = context.getName();
                                        info.additional_info = context.getProperties().get("AdditionalInfo");
                                        info.is_container = context.isContainer();
                                        info.has_state = context.hasState();
                                        info.is_attached = true;
                                        infos.add(info);
                                    }
                                    pending.remove(token);
                                    if (!pending.isEmpty()) return;
                                    doneLoadChildren(channel, parent, null, infos.toArray(new ContextInfo[infos.size()]));
                                }
                            }));
                        }
                    }
                    else {
                        doneLoadChildren(channel, parent, null, new ContextInfo[0]);
                    }
                }
            });
        }
    }

    private void doneLoadChildren(final IChannel channel, final ContextInfo parent, final Throwable error, final ContextInfo[] children) {
        assert Protocol.isDispatchThread();
        assert error == null || children == null;
        if (display == null) return;
        final String state = getChannelState(channel);
        display.asyncExec(new Runnable() {
            public void run() {
                if (ContextListControl.this.channel != channel) return;
                assert parent.children_pending;
                parent.children_pending = false;
                if (state == null) {
                    parent.children = children;
                    parent.children_error = error;
                    if (parent.children_reload) {
                        loadChildren(parent);
                    }
                }
                else {
                    parent.children = null;
                    parent.children_reload = false;
                }
                channel_state = state;
                updateItems(parent);
            }
        });
    }

    private String getChannelState(IChannel channel) {
        if (channel == null) return "Not connected";
        switch (channel.getState()) {
        case IChannel.STATE_OPENING: return "Connecting...";
        case IChannel.STATE_CLOSED: return "Disconnected";
        }
        return null;
    }

    private void onContextRemoved(final String id) {
        if (display == null) return;
        display.asyncExec(new Runnable() {
            public void run() {
                ContextInfo info = findInfo(root_info, id);
                if (info != null && info.parent != null) loadChildren(info.parent);
            }
        });
    }

    private void onContextAdded(final String parent_id) {
        if (display == null) return;
        display.asyncExec(new Runnable() {
            public void run() {
                ContextInfo info = findInfo(root_info, parent_id);
                if (info != null) loadChildren(info);
            }
        });
    }

    public String getFullName(ContextInfo info) {
        if (info == null) return null;
        String name = info.name;
        if (name == null) name = info.id;
        if (info.parent == root_info) return "/" + name;
        if (info.parent == null) return null;
        String path = getFullName(info.parent);
        if (path == null) return null;
        return path + '/' + name;
    }

    private ContextInfo findInfoByFullName(ContextInfo parent, String name, boolean expand) {
        if (name == null) return null;
        if (name.startsWith("/")) return findInfoByFullName(root_info, name.substring(1), expand);
        if (parent.children_pending) return null;
        String head = name;
        String tail = null;
        int i = name.indexOf('/');
        if (i >= 0) {
            head = name.substring(0, i);
            tail = name.substring(i + 1);
        }
        ContextInfo[] children = parent.children;
        if (children != null) {
            for (ContextInfo info : children) {
                if (head.equals(info.name)) {
                    if (tail == null) return info;
                    if (expand) {
                        TreeItem item = findItem(info);
                        if (item != null) item.setExpanded(true);
                    }
                    return findInfoByFullName(info, tail, expand);
                }
            }
        }
        else if (expand) {
            loadChildren(parent);
        }
        return null;
    }

    public ContextInfo findInfo(TreeItem item) {
        assert Thread.currentThread() == display.getThread();
        if (item == null) return root_info;
        return (ContextInfo)item.getData("TCFContextInfo");
    }

    public ContextInfo findInfo(ContextInfo parent, String id) {
        assert Thread.currentThread() == display.getThread();
        if (id == null) return root_info;
        if (id.equals(parent.id)) return parent;
        ContextInfo[] children = parent.children;
        if (children != null) {
            for (ContextInfo info : children) {
                ContextInfo found = findInfo(info, id);
                if (found != null) return found;
            }
        }
        return null;
    }

    private TreeItem findItem(ContextInfo info) {
        assert Thread.currentThread() == display.getThread();
        if (info == null) return null;
        assert info.parent != null;
        if (info.parent == root_info) {
            TreeItem[] items = ctx_tree.getItems();
            for (TreeItem item : items) {
                if (item.getData("TCFContextInfo") == info) return item;
            }
            return null;
        }
        TreeItem parent = findItem(info.parent);
        if (parent == null) return null;
        TreeItem[] items = parent.getItems();
        for (TreeItem item : items) {
            if (item.getData("TCFContextInfo") == info) return item;
        }
        return null;
    }

    private void fillItem(TreeItem item, ContextInfo info) {
        assert Thread.currentThread() == display.getThread();
        Object data = item.getData("TCFContextInfo");
        if (data != null && data != info) item.removeAll();
        item.setData("TCFContextInfo", info);
        StringBuffer bf = new StringBuffer();
        if (info.pid > 0) {
            bf.append(info.pid);
            bf.append(": ");
        }
        if (info.cmd_line != null && info.cmd_line.length > 0) {
            for (String s : info.cmd_line) {
                bf.append(' ');
                bf.append(s);
            }
        }
        else {
            if (info.name != null) {
                bf.append(info.name);
            }
            else {
                bf.append(info.id);
            }
            if (info.additional_info != null) {
                bf.append(info.additional_info.toString());
            }
        }
        item.setText(bf.toString());
        item.setForeground(display.getSystemColor(SWT.COLOR_LIST_FOREGROUND));
        item.setImage(getImage(info));
        if (!canHaveChildren(info)) item.setItemCount(0);
        else if (info.children == null || info.children_error != null) item.setItemCount(1);
        else item.setItemCount(info.children.length);
    }

    private boolean canHaveChildren(ContextInfo info) {
        return !info.has_state && (info.is_container || info == root_info);
    }

    private Image getImage(ContextInfo info) {
        return ImageCache.getImage(info.has_state ? ImageCache.IMG_THREAD_UNKNOWN_STATE : ImageCache.IMG_PROCESS_RUNNING);
    }

    public void setInitialSelection(String full_name) {
        if (full_name == null) return;
        if (full_name.length() == 0) return;
        initial_selection = full_name;
        ContextInfo info = findInfoByFullName(null, full_name, true);
        if (info != null) {
            TreeItem item = findItem(info);
            if (item != null) {
                ctx_tree.setSelection(item);
                initial_selection = null;
            }
        }
    }
}
