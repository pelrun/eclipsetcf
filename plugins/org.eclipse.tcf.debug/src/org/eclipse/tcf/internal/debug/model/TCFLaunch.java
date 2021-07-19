/*******************************************************************************
 * Copyright (c) 2007-2021 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.tcf.internal.debug.Activator;
import org.eclipse.tcf.internal.debug.actions.TCFAction;
import org.eclipse.tcf.internal.debug.launch.TCFLaunchDelegate;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IService;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IContextQuery;
import org.eclipse.tcf.services.IDPrintf;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.services.IFileSystem.IFileHandle;
import org.eclipse.tcf.services.IMemory;
import org.eclipse.tcf.services.IMemoryMap;
import org.eclipse.tcf.services.IPathMap;
import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.services.IProcesses.ProcessContext;
import org.eclipse.tcf.services.IProcessesV1;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.services.IRunControl.RunControlContext;
import org.eclipse.tcf.services.IStreams;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;

/**
 * TCFLaunch class represents an active TCF debug connection.
 * The class handles initialization and synchronization of Memory Map and Path Map services,
 * supports downloading and starting a remote process, maintains breakpoint status information, etc.
 */
public class TCFLaunch extends Launch {

    /**
     * Clients can use LaunchListener interface for notifications of launch being created, connected or
     * disconnected. The interface also allows to receive remote process output.
     */
    public interface LaunchListener {

        public void onCreated(TCFLaunch launch);

        public void onConnected(TCFLaunch launch);

        public void onDisconnected(TCFLaunch launch);

        public void onProcessOutput(TCFLaunch launch, String process_id, int stream_id, byte[] data);

        public void onProcessStreamError(
                TCFLaunch launch, String process_id, int stream_id,
                Exception error, int lost_size);
    }

    /**
     * Launch object handles queue of user actions, like debugger stepping commands.
     * ActionsListener allows clients to be notified when an action execution is started and finished.
     */
    public interface ActionsListener {

        public void onContextActionStart(TCFAction action);

        public void onContextActionResult(String id, String result);

        public void onContextActionDone(TCFAction action);
    }

    private abstract class LaunchStep implements Runnable {

        LaunchStep() {
            launch_steps.add(this);
        }

        abstract void start() throws Exception;

        void done() {
            if (channel.getState() != IChannel.STATE_OPEN) return;
            try {
                launch_steps.removeFirst().start();
            }
            catch (Throwable x) {
                channel.terminate(x);
            }
        }

        public void run() {
            done();
        }
    }

    private static final Collection<LaunchListener> listeners = new ArrayList<LaunchListener>();
    private static LaunchListener[] listeners_array;

    private final Collection<ActionsListener> action_listeners = new ArrayList<ActionsListener>();

    private TCFTask<Boolean> launch_task;
    private IProgressMonitor launch_monitor;

    private IChannel channel;
    private Throwable error;
    private TCFBreakpointsStatus breakpoints_status;
    private String mode;
    private boolean connecting;
    private boolean disconnecting;
    private boolean disconnected;
    private boolean shutdown;
    private boolean last_context_exited;
    private long actions_interval;

    private final LinkedList<TCFTask<Boolean>> disconnect_wait_list = new LinkedList<TCFTask<Boolean>>();

    private final HashSet<Object> pending_clients = new HashSet<Object>();
    private long pending_clients_timestamp;

    private String peer_name;

    private Runnable update_memory_maps;

    private ProcessContext process;
    private Collection<Map<String,Object>> process_signals;
    private boolean process_exited;
    private int process_exit_code;
    private final HashMap<String,String> process_env = new HashMap<String,String>();

    private final HashMap<String,TCFAction> active_actions = new HashMap<String,TCFAction>();
    private final HashMap<String,LinkedList<TCFAction>> context_action_queue = new HashMap<String,LinkedList<TCFAction>>();
    private final HashMap<String,Long> context_action_timestamps = new HashMap<String,Long>();
    private final HashMap<String,ProcessContext> attached_processes = new HashMap<String,ProcessContext>();
    private final HashMap<String,String> process_stream_ids = new HashMap<String,String>();
    private final HashMap<String,String> uart_tx_stream_ids = new HashMap<String,String>();
    private final HashMap<String,String> uart_rx_stream_ids = new HashMap<String,String>();
    private final HashSet<String> disconnected_stream_ids = new HashSet<String>();
    private final LinkedList<LaunchStep> launch_steps = new LinkedList<LaunchStep>();
    private final LinkedList<String> redirection_path = new LinkedList<String>();

    private List<IPathMap.PathMapRule> host_path_map = new ArrayList<IPathMap.PathMapRule>();
    private TCFDataCache<IPathMap.PathMapRule[]> target_path_map;

    private HashMap<String,IStorage> target_path_mapping_cache = new HashMap<String,IStorage>();

    private final HashMap<String,TCFDataCache<String[]>> context_query_cache = new HashMap<String,TCFDataCache<String[]>>();

    private Set<String> context_filter;

    private String dprintf_stream_id;

    private boolean can_terminate_attached = false;

    private InputStream stdin_file_stream;
    private OutputStream stdout_file_stream;

    private final IStreams.StreamsListener streams_listener = new IStreams.StreamsListener() {

        public void created(String stream_type, String stream_id, String context_id) {
            disconnected_stream_ids.remove(stream_id);
            if (stream_type.equals("UART-TX")) {
                uart_tx_stream_ids.put(stream_id, context_id);
                readStream(context_id, stream_id, 0);
            }
            else if (stream_type.equals("UART-RX")) {
                uart_rx_stream_ids.put(stream_id, context_id);
            }
            else {
                process_stream_ids.put(stream_id, context_id);
                disconnectUnusedStreams();
            }
        }

        public void disposed(String stream_type, String stream_id) {
            disconnected_stream_ids.add(stream_id);
        }
    };

    private final IProcesses.ProcessesListener prs_listener = new IProcesses.ProcessesListener() {

        public void exited(String process_id, int exit_code) {
            if (process != null && process_id.equals(process.getID())) {
                process_exit_code = exit_code;
                process_exited = true;
            }
            attached_processes.remove(process_id);
        }
    };

    private final IRunControl.RunControlListener rc_listener = new IRunControl.RunControlListener() {

        private void flushContextQueryCache() {
            for (TCFDataCache<?> c : context_query_cache.values()) c.reset();
        }

        public void contextAdded(RunControlContext[] contexts) {
            flushContextQueryCache();
        }

        public void contextChanged(RunControlContext[] contexts) {
            flushContextQueryCache();
        }

        public void contextRemoved(String[] context_ids) {
            flushContextQueryCache();
        }

        public void contextSuspended(String context, String pc, String reason, Map<String, Object> params) {
        }

        public void contextResumed(String context) {
        }

        public void containerSuspended(String context, String pc, String reason, Map<String, Object> params, String[] suspended_ids) {
        }

        public void containerResumed(String[] context_ids) {
        }

        public void contextException(String context, String msg) {
        }
    };

    private static LaunchListener[] getListeners() {
        if (listeners_array != null) return listeners_array;
        return listeners_array = listeners.toArray(new LaunchListener[listeners.size()]);
    }

    public TCFLaunch(ILaunchConfiguration launchConfiguration, String mode) {
        super(launchConfiguration, mode, null);
        for (LaunchListener l : getListeners()) l.onCreated(TCFLaunch.this);
    }

    private void openConsoleCaptureFiles() {
        ILaunchConfiguration configuration = getLaunchConfiguration();
        if (configuration == null) return;
        try {
            String stdin_file = configuration.getAttribute(TCFLaunchDelegate.ATTR_CAPTURE_STDIN_FILE, (String)null);
            String stdout_file = configuration.getAttribute(TCFLaunchDelegate.ATTR_CAPTURE_STDOUT_FILE, (String)null);
            if (stdin_file == null && stdout_file == null) return;
            IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
            if (stdin_file != null) {
                stdin_file = manager.performStringSubstitution(stdin_file);
                stdin_file_stream = new FileInputStream(new File(stdin_file));
            }
            if (stdout_file != null) {
                stdout_file = manager.performStringSubstitution(stdout_file);
                boolean append = configuration.getAttribute(TCFLaunchDelegate.ATTR_APPEND_TO_FILE, false);
                IWorkspace workspace = ResourcesPlugin.getWorkspace();
                IWorkspaceRoot root = workspace.getRoot();
                Path path = new Path(stdout_file);
                IFile ifile = root.getFileForLocation(path);
                if (ifile != null) {
                    if (append && ifile.exists()) {
                        ifile.appendContents(new ByteArrayInputStream(new byte[0]), true, true, new NullProgressMonitor());
                    }
                    else {
                        if (ifile.exists()) ifile.delete(true, new NullProgressMonitor());
                        ifile.create(new ByteArrayInputStream(new byte[0]), true, new NullProgressMonitor());
                    }
                }
                stdout_file_stream = new FileOutputStream(new File(stdout_file), append);
            }
        }
        catch (Exception x) {
            channel.terminate(new Exception("Cannot open console capture file", x));
        }
    }

    private void closeConsoleCaptureFiles() {
        try {
            if (stdin_file_stream != null) stdin_file_stream.close();
            if (stdout_file_stream != null) stdout_file_stream.close();
        }
        catch (IOException x) {
            Activator.log("Cannot close console capture file", x);
        }
        stdout_file_stream = null;
        stdin_file_stream = null;
    }

    private void onConnected() throws Exception {
        // The method is called when TCF channel is successfully connected.

        if (redirection_path.size() > 0) {
            // Connected to intermediate peer (value-add).
            // Redirect to next peer:
            new LaunchStep() {
                @Override
                void start() throws Exception {
                    String id = redirection_path.removeFirst();
                    IPeer p = Protocol.getLocator().getPeers().get(id);
                    if (p != null) channel.redirect(p.getAttributes());
                    else channel.redirect(id);
                    if (launch_monitor != null) {
                        String name = null;
                        if (p != null) name = p.getName();
                        if (name == null) name = id;
                        launch_monitor.subTask("Connecting to " + name);
                    }
                }
            };
        }
        else {
            final ILaunchConfiguration cfg = getLaunchConfiguration();

            boolean use_context_filter =
                getAttribute("attach_to_context") != null ||
                getAttribute("attach_to_process") != null ||
                cfg != null && cfg.getAttribute(TCFLaunchDelegate.ATTR_LOCAL_PROGRAM_FILE, "").length() > 0 ||
                cfg != null && cfg.getAttribute(TCFLaunchDelegate.ATTR_REMOTE_PROGRAM_FILE, "").length() > 0;
            if (cfg != null) use_context_filter = cfg.getAttribute(TCFLaunchDelegate.ATTR_USE_CONTEXT_FILTER, use_context_filter);
            if (use_context_filter) context_filter = new HashSet<String>();

            final IRunControl rc_service = getService(IRunControl.class);
            if (rc_service != null) rc_service.addListener(rc_listener);

            final IPathMap path_map_service = getService(IPathMap.class);
            if (path_map_service != null) {
                target_path_map = new TCFDataCache<IPathMap.PathMapRule[]>(channel) {
                    @Override
                    protected boolean startDataRetrieval() {
                        command = path_map_service.get(new IPathMap.DoneGet() {
                            public void doneGet(IToken token, Exception error, IPathMap.PathMapRule[] map) {
                                set(token, error, map);
                            }
                        });
                        return false;
                    }
                };
                path_map_service.addListener(new IPathMap.PathMapListener() {
                    public void changed() {
                        target_path_map.reset();
                        target_path_mapping_cache = new HashMap<String,IStorage>();
                    }
                });
            }

            if (cfg != null && path_map_service != null) {
                new LaunchStep() {
                    @Override
                    void start() throws Exception {
                        readPathMapConfiguration(cfg);
                        applyPathMap(this);
                    }
                };
            }

            final IStreams streams = getService(IStreams.class);
            if (streams != null) {
                // Subscribe Streams service:
                new LaunchStep() {
                    @Override
                    void start() {
                        final Set<IToken> cmds = new HashSet<IToken>();
                        String[] nms = { IProcesses.NAME, IProcessesV1.NAME, "UART-RX", "UART-TX" };
                        for (String s : nms) {
                            cmds.add(streams.subscribe(s, streams_listener, new IStreams.DoneSubscribe() {
                                public void doneSubscribe(IToken token, Exception error) {
                                    cmds.remove(token);
                                    if (error != null) channel.terminate(error);
                                    if (cmds.size() == 0) done();
                                }
                            }));
                        }
                        if (cmds.size() == 0) done();
                    }
                };
            }

            if (mode.equals(ILaunchManager.DEBUG_MODE)) {
                if (context_filter != null) {
                    String attach_to_context = getAttribute("attach_to_context");
                    if (attach_to_context != null) context_filter.add(attach_to_context);
                }
                final IMemoryMap mem_map = channel.getRemoteService(IMemoryMap.class);
                if (mem_map != null) {
                    // Send manual memory map items:
                    new LaunchStep() {
                        @Override
                        void start() throws Exception {
                            downloadMemoryMaps(cfg, this);
                        }
                    };
                }
                // Send breakpoints:
                new LaunchStep() {
                    @Override
                    void start() throws Exception {
                        breakpoints_status = new TCFBreakpointsStatus(TCFLaunch.this);
                        Activator.getBreakpointsModel().downloadBreakpoints(channel, this);
                    }
                };
                final IDPrintf dprintf = getService(IDPrintf.class);
                if (dprintf != null) {
                    // Open dprintf stream:
                    new LaunchStep() {
                        @Override
                        void start() throws Exception {
                            dprintf.open(null, new IDPrintf.DoneCommandOpen() {
                                @Override
                                public void doneCommandOpen(IToken token, Exception error, final String id) {
                                    if (error != null) {
                                        channel.terminate(error);
                                        return;
                                    }
                                    dprintf_stream_id = id;
                                    streams.connect(id, new IStreams.DoneConnect() {
                                        @Override
                                        public void doneConnect(IToken token, Exception error) {
                                            if (error != null) {
                                                channel.terminate(error);
                                                return;
                                            }
                                            readStream(null, id, 0);
                                            done();
                                        }
                                    });
                                }
                            });
                        }
                    };
                }
            }

            if (cfg != null && getService(IMemory.class) != null) {
                String s = cfg.getAttribute(TCFLaunchDelegate.ATTR_FILES, (String)null);
                if (s != null) {
                    @SuppressWarnings("unchecked")
                    Collection<Map<String,Object>> c = (Collection<Map<String,Object>>)JSON.parseOne(s.getBytes("UTF-8"));
                    final ElfLoader loader = new ElfLoader(channel);
                    for (final Map<String,Object> m : c) {
                        Boolean b1 = (Boolean)m.get(TCFLaunchDelegate.FILES_DOWNLOAD);
                        Boolean b2 = (Boolean)m.get(TCFLaunchDelegate.FILES_SET_PC);
                        if (b1 != null && b1.booleanValue() || b2 != null && b2.booleanValue()) {
                            new LaunchStep() {
                                @Override
                                void start() throws Exception {
                                    loader.load(m, this);
                                }
                            };
                        }
                    }
                    new LaunchStep() {
                        @Override
                        void start() throws Exception {
                            loader.dispose();
                            done();
                        }
                    };
                }
            }

            // Call client launch sequence:
            new LaunchStep() {
                @Override
                void start() {
                    runLaunchSequence(this);
                }
            };

            if (cfg != null) startRemoteProcess(cfg);

            // Final launch step.
            // Notify clients:
            new LaunchStep() {
                @Override
                void start() {
                    connecting = false;
                    disconnectUnusedStreams();
                    openConsoleCaptureFiles();
                    for (LaunchListener l : getListeners()) l.onConnected(TCFLaunch.this);
                    fireChanged();
                    if (launch_task != null) launch_task.done(true);
                    launch_monitor = null;
                    launch_task = null;
                }
            };
        }

        launch_steps.removeFirst().start();
    }

    private void onDisconnected(Throwable error) {
        // The method is called when TCF channel is closed.
        assert !disconnected;
        assert !shutdown;
        this.error = error;
        breakpoints_status = null;
        connecting = false;
        disconnected = true;
        for (LaunchListener l : getListeners()) l.onDisconnected(this);
        for (TCFDataCache<?> c : context_query_cache.values()) c.dispose();
        context_query_cache.clear();
        closeConsoleCaptureFiles();
        if (DebugPlugin.getDefault() != null) fireChanged();
        if (launch_task != null) launch_task.done(false);
        launch_monitor = null;
        launch_task = null;
        runShutdownSequence(new Runnable() {
            public void run() {
                shutdown = true;
                fireTerminate();
                for (TCFTask<Boolean> tsk : disconnect_wait_list) {
                    tsk.done(Boolean.TRUE);
                }
                disconnect_wait_list.clear();
            }
        });
        // Log severe exceptions: bug 386067
        if (error instanceof RuntimeException) {
            Activator.log("Channel disconnected with error", error);
        }
    }

    protected void runLaunchSequence(Runnable done) {
        done.run();
    }

    private void downloadMemoryMaps(ILaunchConfiguration cfg, final Runnable done) throws Exception {
        final IMemoryMap mmap = channel.getRemoteService(IMemoryMap.class);
        if (mmap == null) {
            done.run();
            return;
        }
        final HashMap<String,ArrayList<IMemoryMap.MemoryRegion>> maps = new HashMap<String,ArrayList<IMemoryMap.MemoryRegion>>();
        getMemMaps(maps, cfg);
        final HashSet<IToken> cmds = new HashSet<IToken>(); // Pending commands
        final Runnable done_all = new Runnable() {
            boolean launch_done;
            public void run() {
                if (launch_done) return;
                done.run();
                launch_done = true;
            }
        };
        final IMemoryMap.DoneSet done_set_mmap = new IMemoryMap.DoneSet() {
            public void doneSet(IToken token, Exception error) {
                assert cmds.contains(token);
                cmds.remove(token);
                if (error != null) Activator.log("Cannot update context memory map", error);
                if (cmds.isEmpty()) done_all.run();
            }
        };
        for (String id : maps.keySet()) {
            ArrayList<IMemoryMap.MemoryRegion> map = maps.get(id);
            TCFMemoryRegion[] arr = map.toArray(new TCFMemoryRegion[map.size()]);
            cmds.add(mmap.set(id, arr, done_set_mmap));
        }
        update_memory_maps = new Runnable() {
            public void run() {
                try {
                    Set<String> set = new HashSet<String>(maps.keySet());
                    maps.clear();
                    getMemMaps(maps, getLaunchConfiguration());
                    for (String id : maps.keySet()) {
                        ArrayList<IMemoryMap.MemoryRegion> map = maps.get(id);
                        TCFMemoryRegion[] arr = map.toArray(new TCFMemoryRegion[map.size()]);
                        cmds.add(mmap.set(id, arr, done_set_mmap));
                    }
                    for (String id : set) {
                        if (maps.get(id) != null) continue;
                        cmds.add(mmap.set(id, null, done_set_mmap));
                    }
                }
                catch (Throwable x) {
                    channel.terminate(x);
                }
            }
        };
        if (cmds.isEmpty()) done_all.run();
    }

    @SuppressWarnings("unchecked")
    private void getMemMaps(Map<String,ArrayList<IMemoryMap.MemoryRegion>> maps, ILaunchConfiguration cfg) throws Exception {
        // Parse ATTR_FILES
        String s = cfg.getAttribute(TCFLaunchDelegate.ATTR_FILES, (String)null);
        if (s != null) {
            Collection<Map<String,Object>> c = (Collection<Map<String,Object>>)JSON.parseOne(s.getBytes("UTF-8"));
            for (Map<String,Object> m : c) {
                Boolean b = (Boolean)m.get(TCFLaunchDelegate.FILES_LOAD_SYMBOLS);
                if (b != null && b.booleanValue()) {
                    String id = (String)m.get(TCFLaunchDelegate.FILES_CONTEXT_ID);
                    if (id == null) id = (String)m.get(TCFLaunchDelegate.FILES_CONTEXT_FULL_NAME);
                    if (id != null) {
                        Map<String,Object> map = new HashMap<String,Object>();
                        map.put(IMemoryMap.PROP_FILE_NAME, m.get(TCFLaunchDelegate.FILES_FILE_NAME));
                        b = (Boolean)m.get(TCFLaunchDelegate.FILES_RELOCATE);
                        if (b != null && b.booleanValue()) {
                            map.put(IMemoryMap.PROP_ADDRESS, m.get(TCFLaunchDelegate.FILES_ADDRESS));
                            map.put(IMemoryMap.PROP_OFFSET, m.get(TCFLaunchDelegate.FILES_OFFSET));
                            map.put(IMemoryMap.PROP_SIZE, m.get(TCFLaunchDelegate.FILES_SIZE));
                        }
                        b = (Boolean)m.get(TCFLaunchDelegate.FILES_ENABLE_OSA);
                        if (b != null && b.booleanValue()) {
                            map.put(IMemoryMap.PROP_OSA, new HashMap<String,Object>());
                        }
                        ArrayList<IMemoryMap.MemoryRegion> l = maps.get(id);
                        if (l == null) {
                            l = new ArrayList<IMemoryMap.MemoryRegion>();
                            maps.put(id, l);
                        }
                        l.add(new TCFMemoryRegion(map));
                    }
                }
            }
        }
        // Parse ATTR_MEMORY_MAP
        TCFLaunchDelegate.getMemMapsAttribute(maps, cfg);
    }

    private void readPathMapConfiguration(ILaunchConfiguration cfg) throws CoreException {
        String s = cfg.getAttribute(TCFLaunchDelegate.ATTR_PATH_MAP, "");
        host_path_map = new ArrayList<IPathMap.PathMapRule>();
        host_path_map.addAll(TCFLaunchDelegate.parsePathMapAttribute(s));
        s = cfg.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, "");
        host_path_map.addAll(TCFLaunchDelegate.parseSourceLocatorMemento(s));
    }

    /**
     * Returns the TCF client ID of this instance of Eclipse.
     * @return The client ID.
     */
    public String getClientID() {
        return Activator.getClientID();
    }

    /**
     * Apply the path map to this launch channel.
     * @param done The done to invoke.
     */
    protected void applyPathMap(final Runnable done) {
        IPathMap path_map_service = getService(IPathMap.class);
        if (path_map_service == null) {
            if (done != null) done.run();
            return;
        }
        path_map_service.set(host_path_map.toArray(new IPathMap.PathMapRule[host_path_map.size()]), new IPathMap.DoneSet() {
            @Override
            public void doneSet(IToken token, Exception error) {
                if (error != null) channel.terminate(error);
                else if (done != null) done.run();
            }
        });
    }

    private String[] toArgsArray(String file, String cmd) {
        // Create arguments list from a command line.
        int i = 0;
        int l = cmd.length();
        List<String> arr = new ArrayList<String>();
        arr.add(file);
        for (;;) {
            while (i < l && cmd.charAt(i) == ' ') i++;
            if (i >= l) break;
            String s = null;
            if (cmd.charAt(i) == '"') {
                i++;
                StringBuffer bf = new StringBuffer();
                while (i < l) {
                    char ch = cmd.charAt(i++);
                    if (ch == '"') break;
                    if (ch == '\\' && i < l) ch = cmd.charAt(i++);
                    bf.append(ch);
                }
                s = bf.toString();
            }
            else {
                int i0 = i;
                while (i < l && cmd.charAt(i) != ' ') i++;
                s = cmd.substring(i0, i);
            }
            arr.add(s);
        }
        return arr.toArray(new String[arr.size()]);
    }

    private void copyFileToRemoteTarget(String local_file, String remote_file, final Runnable done) {
        if (local_file == null) {
            channel.terminate(new Exception("Program does not exist"));
            return;
        }
        final IFileSystem fs = channel.getRemoteService(IFileSystem.class);
        if (fs == null) {
            channel.terminate(new Exception(
                    "Cannot download program file: target does not provide File System service"));
            return;
        }
        try {
            final File local_fd = new File(local_file);
            final InputStream inp = new FileInputStream(local_fd);
            final String task_name = "Downloading: " + local_fd.getName();
            int flags = IFileSystem.TCF_O_WRITE | IFileSystem.TCF_O_CREAT | IFileSystem.TCF_O_TRUNC;
            if (launch_monitor != null) launch_monitor.subTask(task_name);
            fs.open(remote_file, flags, null, new IFileSystem.DoneOpen() {

                IFileHandle handle;
                long offset = 0;
                final Set<IToken> cmds = new HashSet<IToken>();
                final byte[] buf = new byte[0x1000];

                public void doneOpen(IToken token, FileSystemException error, IFileHandle handle) {
                    this.handle = handle;
                    if (error != null) {
                        TCFLaunch.this.error = new Exception("Cannot download program file", error);
                        fireChanged();
                        done.run();
                    }
                    else {
                        write_next();
                    }
                }

                private void write_next() {
                    try {
                        while (cmds.size() < 8) {
                            int rd = inp.read(buf);
                            if (rd < 0) {
                                close();
                                break;
                            }
                            final long kb_done = (offset + rd) / 1024;
                            cmds.add(fs.write(handle, offset, buf, 0, rd, new IFileSystem.DoneWrite() {

                                public void doneWrite(IToken token, FileSystemException error) {
                                    cmds.remove(token);
                                    if (launch_monitor != null) {
                                        launch_monitor.subTask(task_name + ", " + kb_done + " KB done");
                                    }
                                    if (error != null) channel.terminate(error);
                                    else write_next();
                                }
                            }));
                            offset += rd;
                        }
                    }
                    catch (Throwable x) {
                        channel.terminate(x);
                    }
                }

                private void close() {
                    if (cmds.size() > 0) return;
                    try {
                        inp.close();
                        fs.close(handle, new IFileSystem.DoneClose() {

                            public void doneClose(IToken token, FileSystemException error) {
                                if (error != null) channel.terminate(error);
                                else done.run();
                            }
                        });
                    }
                    catch (Throwable x) {
                        channel.terminate(x);
                    }
                }
            });
        }
        catch (Throwable x) {
            channel.terminate(x);
        }
    }

    private void startRemoteProcess(final ILaunchConfiguration cfg) throws Exception {
        final String project = cfg.getAttribute(TCFLaunchDelegate.ATTR_PROJECT_NAME, "");
        final String local_file = cfg.getAttribute(TCFLaunchDelegate.ATTR_LOCAL_PROGRAM_FILE, "");
        final String remote_file = cfg.getAttribute(TCFLaunchDelegate.ATTR_REMOTE_PROGRAM_FILE, "");
        if (local_file.length() != 0 && remote_file.length() != 0) {
            // Download executable file
            new LaunchStep() {
                @Override
                void start() throws Exception {
                    copyFileToRemoteTarget(TCFLaunchDelegate.getProgramPath(project, local_file), remote_file, this);
                }
            };
        }
        final String attach_to_process = getAttribute("attach_to_process");
        if (attach_to_process != null) {
            final IProcesses ps = channel.getRemoteService(IProcesses.class);
            if (ps == null) throw new Exception("Target does not provide Processes service");
            // Attach the process
            new LaunchStep() {
                @Override
                void start() {
                    IProcesses.DoneGetContext done = new IProcesses.DoneGetContext() {
                        public void doneGetContext(IToken token, final Exception error, final ProcessContext process) {
                            if (error != null) {
                                channel.terminate(error);
                            }
                            else {
                                process.attach(new IProcesses.DoneCommand() {
                                    public void doneCommand(IToken token, final Exception error) {
                                        if (error != null) {
                                            channel.terminate(error);
                                        }
                                        else {
                                            TCFLaunch.this.process = process;
                                            ps.addListener(prs_listener);
                                            onAttach(process);
                                            done();
                                        }
                                    }
                                });
                            }
                        }
                    };
                    ps.getContext(attach_to_process, done);
                }
            };
        }
        else if (local_file.length() != 0 || remote_file.length() != 0) {
            final IProcesses ps = channel.getRemoteService(IProcesses.class);
            if (ps == null) throw new Exception("Target does not provide Processes service");
            final boolean append = cfg.getAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
            if (append) {
                // Get system environment variables
                new LaunchStep() {
                    @Override
                    void start() throws Exception {
                        ps.getEnvironment(new IProcesses.DoneGetEnvironment() {
                            public void doneGetEnvironment(IToken token, Exception error, Map<String,String> env) {
                                if (error != null) {
                                    channel.terminate(error);
                                }
                                else {
                                    if (env != null) process_env.putAll(env);
                                    done();
                                }
                            }
                        });
                    }
                };
            }
            final String dir = cfg.getAttribute(TCFLaunchDelegate.ATTR_WORKING_DIRECTORY, "");
            final String args = cfg.getAttribute(TCFLaunchDelegate.ATTR_PROGRAM_ARGUMENTS, "");
            final Map<String,String> env = cfg.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, (Map<String,String>)null);
            final boolean attach_children = cfg.getAttribute(TCFLaunchDelegate.ATTR_ATTACH_CHILDREN, true);
            final boolean stop_at_entry = cfg.getAttribute(TCFLaunchDelegate.ATTR_STOP_AT_ENTRY, true);
            final boolean stop_at_main = cfg.getAttribute(TCFLaunchDelegate.ATTR_STOP_AT_MAIN, true);
            final boolean use_terminal = cfg.getAttribute(TCFLaunchDelegate.ATTR_USE_TERMINAL, true);
            final Set<Integer> dont_stop = TCFLaunchDelegate.readSigSet(cfg.getAttribute(TCFLaunchDelegate.ATTR_SIGNALS_DONT_STOP, ""));
            final Set<Integer> dont_pass = TCFLaunchDelegate.readSigSet(cfg.getAttribute(TCFLaunchDelegate.ATTR_SIGNALS_DONT_PASS, ""));
            final IProcessesV1 ps_v1 = channel.getRemoteService(IProcessesV1.class);
            if (ps_v1 != null) {
                // Get processes service capabilities
                new LaunchStep() {
                    @Override
                    void start() throws Exception  {
                        ps_v1.getCapabilities(null, new IProcessesV1.DoneGetCapabilities() {
                            @Override
                            public void doneGetCapabilities(IToken token, Exception error, Map<String,Object> properties) {
                                can_terminate_attached = properties != null &&
                                        properties.get("CanTerminateAttached") instanceof Boolean &&
                                        ((Boolean)properties.get("CanTerminateAttached")).booleanValue();
                                done();
                            }
                        });
                    }
                };
            }
            // Start the process
            new LaunchStep() {
                @Override
                void start() throws Exception  {
                    if (env != null) {
                        for (Map.Entry<String,String> e : env.entrySet()) {
                            String key = e.getKey();
                            String val = e.getValue();
                            if (val == null) {
                                process_env.remove(key);
                            }
                            else {
                                val = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(val);
                                process_env.put(key, val);
                            }
                        }
                    }
                    String file = remote_file;
                    if (file == null || file.length() == 0) file = TCFLaunchDelegate.getProgramPath(project, local_file);
                    if (file == null || file.length() == 0) {
                        channel.terminate(new Exception("Program file does not exist"));
                        return;
                    }
                    final String prog_name = file.length() < 32 ? file : "..." + file.substring(file.length() - 30);
                    IProcesses.DoneStart done = new IProcesses.DoneStart() {
                        public void doneStart(IToken token, final Exception error, ProcessContext process) {
                            if (error != null) {
                                Protocol.sync(new Runnable() {
                                    public void run() {
                                        channel.terminate(new Exception("Cannot launch '" + prog_name + "'", error));
                                    }
                                });
                            }
                            else {
                                TCFLaunch.this.process = process;
                                ps.addListener(prs_listener);
                                onAttach(process);
                                done();
                            }
                        }
                    };
                    if (launch_monitor != null) launch_monitor.subTask("Starting: " + file);
                    String cmd = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(args);
                    String[] args_arr = toArgsArray(file, cmd);
                    if (ps_v1 != null) {
                        Map<String,Object> params = new HashMap<String,Object>();
                        if (mode.equals(ILaunchManager.DEBUG_MODE)) {
                            params.put(IProcessesV1.START_ATTACH, true);
                            params.put(IProcessesV1.START_ATTACH_CHILDREN, attach_children);
                            params.put(IProcessesV1.START_STOP_AT_ENTRY, stop_at_entry);
                            params.put(IProcessesV1.START_STOP_AT_MAIN, stop_at_main);
                            if (dont_stop.size() > 0) params.put(IProcessesV1.START_SIG_DONT_STOP, dont_stop);
                            if (dont_pass.size() > 0) params.put(IProcessesV1.START_SIG_DONT_PASS, dont_pass);
                        }
                        if (use_terminal) params.put(IProcessesV1.START_USE_TERMINAL, true);
                        ps_v1.start(dir, file, args_arr, process_env, params, done);
                    }
                    else {
                        boolean attach = mode.equals(ILaunchManager.DEBUG_MODE);
                        ps.start(dir, file, args_arr, process_env, attach, done);
                    }
                }
            };
            if (mode.equals(ILaunchManager.DEBUG_MODE)) {
                // Get process signal list
                new LaunchStep() {
                    @Override
                    void start() {
                        ps.getSignalList(process.getID(), new IProcesses.DoneGetSignalList() {
                            public void doneGetSignalList(IToken token, Exception error, Collection<Map<String,Object>> list) {
                                if (error != null && attached_processes.get(process.getID()) != null) {
                                    Activator.log("Can't get process signal list", error);
                                }
                                process_signals = list;
                                done();
                            }
                        });
                    }
                };
                // Set process signal masks
                if (ps_v1 == null && (dont_stop.size() > 0 || dont_pass.size() > 0)) {
                    new LaunchStep() {
                        @Override
                        void start() {
                            final HashSet<IToken> cmds = new HashSet<IToken>();
                            final IProcesses.DoneCommand done_set_mask = new IProcesses.DoneCommand() {
                                public void doneCommand(IToken token, Exception error) {
                                    cmds.remove(token);
                                    if (error != null && attached_processes.size() > 0) channel.terminate(error);
                                    else if (cmds.size() == 0) done();
                                }
                            };
                            cmds.add(ps.setSignalMask(process.getID(), dont_stop, dont_pass, done_set_mask));
                            final IRunControl rc = channel.getRemoteService(IRunControl.class);
                            if (rc != null) {
                                final IRunControl.DoneGetChildren done_get_children = new IRunControl.DoneGetChildren() {
                                    public void doneGetChildren(IToken token, Exception error, String[] context_ids) {
                                        if (context_ids != null) {
                                            for (String id : context_ids) {
                                                cmds.add(ps.setSignalMask(id, dont_stop, dont_pass, done_set_mask));
                                                cmds.add(rc.getChildren(id, this));
                                            }
                                        }
                                        cmds.remove(token);
                                        if (error != null && attached_processes.size() > 0) channel.terminate(error);
                                        else if (cmds.size() == 0) done();
                                    }
                                };
                                cmds.add(rc.getChildren(process.getID(), done_get_children));
                            }
                        }
                    };
                }
            }
        }
    }

    private void readProcessStreams(ProcessContext ctx) {
        assert attached_processes.get(ctx.getID()) == ctx;
        IStreams streams = getService(IStreams.class);
        if (streams == null) return;
        String out_id = (String)ctx.getProperties().get(IProcesses.PROP_STDOUT_ID);
        String err_id = (String)ctx.getProperties().get(IProcesses.PROP_STDERR_ID);
        if (process_stream_ids.get(out_id) != null) readStream(ctx.getID(), out_id, 0);
        if (process_stream_ids.get(err_id) != null) readStream(ctx.getID(), err_id, 0);
    }

    private void disconnectUnusedStreams() {
        if (connecting) return;
        HashSet<String> set = new HashSet<String>();
        for (ProcessContext ctx : attached_processes.values()) {
            set.add((String)ctx.getProperties().get(IProcesses.PROP_STDIN_ID));
            set.add((String)ctx.getProperties().get(IProcesses.PROP_STDOUT_ID));
            set.add((String)ctx.getProperties().get(IProcesses.PROP_STDERR_ID));
        }
        for (String id : process_stream_ids.keySet().toArray(new String[process_stream_ids.size()])) {
            if (!set.contains(id)) disconnectStream(id);
        }
    }

    private void readStream(final String ctx_id, final String id, final int no) {
        if (ctx_id != null) {
            // Force creation of console
            for (LaunchListener l : getListeners()) l.onProcessOutput(this, ctx_id, no, null);
            // Read console input file
            if (stdin_file_stream != null) {
                try {
                    byte[] buf = new byte[256];
                    for (;;) {
                        int rd = stdin_file_stream.read(buf);
                        if (rd <= 0) break;
                        writeProcessInputStream(ctx_id, buf, 0, rd);
                    }
                    stdin_file_stream.close();
                }
                catch (Exception x) {
                    Activator.log("Cannot read console capture file", x);
                }
                stdin_file_stream = null;
            }
        }
        final IStreams streams = getService(IStreams.class);
        IStreams.DoneRead done = new IStreams.DoneRead() {
            public void doneRead(IToken token, Exception error, int lost_size, byte[] data, boolean eos) {
                if (lost_size > 0) {
                    Exception x = new IOException("Process output data lost due buffer overflow");
                    for (LaunchListener l : getListeners()) l.onProcessStreamError(TCFLaunch.this, ctx_id, no, x, lost_size);
                }
                if (data != null && data.length > 0) {
                    for (LaunchListener l : getListeners()) l.onProcessOutput(TCFLaunch.this, ctx_id, no, data);
                    if (stdout_file_stream != null && data != null) {
                        try {
                            stdout_file_stream.write(data);
                        }
                        catch (IOException x) {
                            Activator.log("Cannot write console capture file", x);
                            try {
                                stdout_file_stream.close();
                            }
                            catch (IOException y) {
                                Activator.log("Cannot close console capture file", y);
                            }
                            stdout_file_stream = null;
                        }
                    }
                }
                if (disconnected_stream_ids.contains(id)) return;
                if (error != null) {
                    for (LaunchListener l : getListeners()) l.onProcessStreamError(TCFLaunch.this, ctx_id, no, error, 0);
                    disconnected_stream_ids.add(id);
                }
                if (!eos && error == null) {
                    streams.read(id, 0x1000, this);
                }
            }
        };
        streams.read(id, 0x1000, done);
        streams.read(id, 0x1000, done);
        streams.read(id, 0x1000, done);
        streams.read(id, 0x1000, done);
    }

    private void disconnectStream(String id) {
        assert process_stream_ids.get(id) != null;
        process_stream_ids.remove(id);
        if (channel.getState() != IChannel.STATE_OPEN) return;
        disconnected_stream_ids.add(id);
        IStreams streams = getService(IStreams.class);
        streams.disconnect(id, new IStreams.DoneDisconnect() {
            public void doneDisconnect(IToken token, Exception error) {
                if (channel.getState() != IChannel.STATE_OPEN) return;
                if (error != null) channel.terminate(error);
            }
        });
    }

    protected void runShutdownSequence(final Runnable done) {
        done.run();
    }

    /*--------------------------------------------------------------------------------------------*/

    /**
     * Return error object if launching failed.
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Terminate the launch because of fatal error.
     * @param x - the error object.
     */
    public void setError(Throwable x) {
        error = x;
        if (x != null) {
            if (channel != null && channel.getState() == IChannel.STATE_OPEN) {
                channel.terminate(x);
            }
            else if (!connecting) {
                disconnected = true;
                shutdown = true;
            }
        }
        fireChanged();
    }

    /**
     * Get current target breakpoints status information.
     * @return status information object
     */
    public TCFBreakpointsStatus getBreakpointsStatus() {
        return breakpoints_status;
    }

    /**
     * Check if the agent supports setting of user defined memory map entries
     * for a context that does not exits yet.
     * @return true if memory map preloading is supported.
     */
    public boolean isMemoryMapPreloadingSupported() {
        return true;
    }

    /**
     * Register a launch listener.
     * @param listener - client object implementing TCFLaunch.LaunchListener interface.
     */
    public static void addListener(LaunchListener listener) {
        assert Protocol.isDispatchThread();
        listeners.add(listener);
        listeners_array = null;
    }

    /**
     * Remove a launch listener.
     * @param listener - client object implementing TCFLaunch.LaunchListener interface.
     */
    public static void removeListener(LaunchListener listener) {
        assert Protocol.isDispatchThread();
        listeners.remove(listener);
        listeners_array = null;
    }

    @Override
    public void launchConfigurationChanged(final ILaunchConfiguration cfg) {
        super.launchConfigurationChanged(cfg);
        if (!cfg.equals(getLaunchConfiguration())) return;
        if (channel != null && channel.getState() == IChannel.STATE_OPEN && !connecting) {
            new TCFTask<Boolean>(channel) {
                public void run() {
                    try {
                        if (update_memory_maps != null) update_memory_maps.run();
                        readPathMapConfiguration(cfg);
                        applyPathMap(new Runnable() {
                            public void run() {
                                done(false);
                            }
                        });
                    }
                    catch (Throwable x) {
                        channel.terminate(x);
                        done(false);
                    }
                }
            }.getE();
            // TODO: update signal masks when launch configuration changes
        }
    }

    @Override
    public void launchRemoved(ILaunch launch) {
        if (this != launch) return;
        super.launchRemoved(launch);
        Protocol.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                if (channel != null && channel.getState() != IChannel.STATE_CLOSED) {
                    channel.close();
                }
            }
        });
    }

    /**
     * Get TCF communication channel that is used by the launch.
     * Thread safe method.
     */
    public IChannel getChannel() {
        return channel;
    }

    /**
     * If the launch has started a remote process, return the process information.
     * Starting a process is optional and not applicable to all launches.
     * @return remote process information or null.
     */
    public IProcesses.ProcessContext getProcessContext() {
        return process;
    }

    /**
     * Write to stdin stream of remote process that was started by the launch.
     * @param prs_id - TCF ID of the process.
     * @param buf - data to write
     * @param pos - starting position in 'buf'
     * @param len - number of bytes to write.
     * @throws Exception
     */
    public void writeProcessInputStream(final String prs_id, byte[] buf, int pos, final int len) throws Exception {
        assert Protocol.isDispatchThread();
        if (channel.getState() != IChannel.STATE_OPEN) throw new IOException("Connection closed");
        IStreams streams = getService(IStreams.class);
        if (streams == null) throw new IOException("Streams service not available");
        ProcessContext ctx = attached_processes.get(prs_id);
        if (ctx != null) {
            final String id = (String)ctx.getProperties().get(IProcesses.PROP_STDIN_ID);
            if (process_stream_ids.get(id) == null) throw new IOException("Input stream not available");
            streams.write(id, buf, pos, len, new IStreams.DoneWrite() {
                public void doneWrite(IToken token, Exception error) {
                    if (error == null) return;
                    if (process_stream_ids.get(id) == null) return;
                    for (LaunchListener l : getListeners()) l.onProcessStreamError(TCFLaunch.this, prs_id, 0, error, len);
                    disconnectStream(id);
                }
            });
            return;
        }
        for (final String rx_id : uart_rx_stream_ids.keySet()) {
            if (!prs_id.equals(uart_rx_stream_ids.get(rx_id))) continue;
            streams.write(rx_id, buf, pos, len, new IStreams.DoneWrite() {
                public void doneWrite(IToken token, Exception error) {
                    if (error == null) return;
                    if (uart_rx_stream_ids.get(rx_id) == null) return;
                    for (LaunchListener l : getListeners()) l.onProcessStreamError(TCFLaunch.this, prs_id, 0, error, len);
                    disconnectStream(rx_id);
                }
            });
            return;
        }
        throw new IOException("No target process");
    }

    public void openUartStreams(final String ctx_id, Map<String,Object> uart_props) {
        assert Protocol.isDispatchThread();
        if (uart_props == null) return;
        IStreams streams = getService(IStreams.class);
        if (streams == null) return;
        final String rx_id = (String)uart_props.get("RXStreamID");
        if (rx_id != null && uart_rx_stream_ids.get(rx_id) == null) {
            streams.connect(rx_id, new IStreams.DoneConnect() {
                @Override
                public void doneConnect(IToken token, Exception error) {
                    if (uart_rx_stream_ids.get(rx_id) != null) return;
                    uart_rx_stream_ids.put(rx_id, ctx_id);
                    if (error == null) return;
                    for (LaunchListener l : getListeners()) l.onProcessStreamError(TCFLaunch.this, ctx_id, 0, error, 0);
                }
            });
        }
        final String tx_id = (String)uart_props.get("TXStreamID");
        if (tx_id != null && uart_tx_stream_ids.get(tx_id) == null) {
            streams.connect(tx_id, new IStreams.DoneConnect() {
                @Override
                public void doneConnect(IToken token, Exception error) {
                    if (uart_tx_stream_ids.get(tx_id) != null) return;
                    uart_tx_stream_ids.put(tx_id, ctx_id);
                    if (error == null) {
                        readStream(ctx_id, tx_id, 0);
                        return;
                    }
                    for (LaunchListener l : getListeners()) l.onProcessStreamError(TCFLaunch.this, ctx_id, 0, error, 0);
                }
            });
        }
    }

    public boolean isConnecting() {
        return connecting;
    }

    public boolean isConnected() {
        return channel != null && !connecting && !disconnected;
    }

    public void onAttach(ProcessContext ctx) {
        if (context_filter != null) context_filter.add(ctx.getID());
        attached_processes.put(ctx.getID(), ctx);
        readProcessStreams(ctx);
    }

    public void onDetach(String prs_id) {
        disconnectUnusedStreams();
    }

    public void onLastContextRemoved() {
        ILaunchConfiguration cfg = getLaunchConfiguration();
        try {
            if (process != null && cfg.getAttribute(TCFLaunchDelegate.ATTR_DISCONNECT_ON_CTX_EXIT, true)) {
                last_context_exited = true;
                closeChannel();
            }
        }
        catch (Throwable e) {
            Activator.log("Cannot access launch configuration", e);
        }
    }

    public void closeChannel() {
        assert Protocol.isDispatchThread();
        if (channel == null) return;
        if (channel.getState() == IChannel.STATE_CLOSED) return;
        if (disconnecting) return;
        disconnecting = true;
        final Set<IToken> cmds = new HashSet<IToken>();
        if (channel.getState() == IChannel.STATE_OPEN) {
            boolean terminate = true;
            try {
                ILaunchConfiguration cfg = getLaunchConfiguration();
                if (cfg != null) terminate = cfg.getAttribute(TCFLaunchDelegate.ATTR_TERMINATE_ON_DISCONNECT, true);
            }
            catch (Exception x) {
                channel.terminate(x);
            }
            if (terminate) {
                for (final ProcessContext ctx : attached_processes.values()) {
                    IProcesses.DoneCommand prs_done_cmd = new IProcesses.DoneCommand() {
                        public void doneCommand(IToken token, Exception error) {
                            cmds.remove(token);
                            if (!attached_processes.containsKey(ctx.getID())) error = null;
                            if (error != null) channel.terminate(error);
                            else if (cmds.isEmpty()) channel.close();
                        }
                    };
                    if (!can_terminate_attached) cmds.add(ctx.detach(prs_done_cmd));
                    cmds.add(ctx.terminate(prs_done_cmd));
                }
            }
            IStreams streams = getService(IStreams.class);
            IStreams.DoneDisconnect done_disconnect = new IStreams.DoneDisconnect() {
                public void doneDisconnect(IToken token, Exception error) {
                    cmds.remove(token);
                    if (error != null) channel.terminate(error);
                    else if (cmds.isEmpty()) channel.close();
                }
            };
            for (String id : process_stream_ids.keySet()) {
                cmds.add(streams.disconnect(id, done_disconnect));
            }
            for (String id : uart_rx_stream_ids.keySet()) {
                cmds.add(streams.disconnect(id, done_disconnect));
            }
            for (String id : uart_tx_stream_ids.keySet()) {
                cmds.add(streams.disconnect(id, done_disconnect));
            }
            process_stream_ids.clear();
            uart_rx_stream_ids.clear();
            uart_tx_stream_ids.clear();
            if (dprintf_stream_id != null) {
                disconnected_stream_ids.add(dprintf_stream_id);
                cmds.add(streams.disconnect(dprintf_stream_id, done_disconnect));
                dprintf_stream_id = null;
            }
        }
        if (cmds.isEmpty()) channel.close();
    }

    public IPeer getPeer() {
        assert Protocol.isDispatchThread();
        return channel.getRemotePeer();
    }

    public String getPeerName() {
        // Safe to call from any thread.
        return peer_name;
    }

    /**
     * Returns the name for the given peer. Overwrite to customize
     * the peer name shown.
     *
     * @param peer The peer. Must not be <code>null</code>.
     * @return The peer name. Must be never <code>null</code>.
     */
    protected String getPeerName(IPeer peer) {
        assert Protocol.isDispatchThread();
        assert peer != null;

        return peer.getName();
    }

    public <V extends IService> V getService(Class<V> cls) {
        assert Protocol.isDispatchThread();
        return channel.getRemoteService(cls);
    }

    @Override
    public boolean canDisconnect() {
        return !disconnected;
    }

    @Override
    public boolean isDisconnected() {
        return disconnected;
    }

    @Override
    public void disconnect() throws DebugException {
        try {
            new TCFTask<Boolean>(8000) {
                public void run() {
                    if (channel == null || shutdown) {
                        done(true);
                    }
                    else {
                        disconnect_wait_list.add(this);
                        closeChannel();
                    }
                }
            }.get();
        }
        catch (IllegalStateException x) {
            // Don't report this exception - it means Eclipse is being shut down
        }
        catch (Exception x) {
            throw new DebugException(new TCFError(x));
        }
    }

    @Override
    public boolean canTerminate() {
        try {
            return new TCFTask<Boolean>(8000) {
                public void run() {
                    done(!disconnected && process != null && process.canTerminate());
                }
            }.get();
        }
        catch (Exception x) {
            return false;
        }
    }

    @Override
    public boolean isTerminated() {
        return disconnected;
    }

    @Override
    public void terminate() throws DebugException {
        try {
            new TCFTask<Boolean>(8000) {
                boolean detached;
                boolean terminated;
                public void run() {
                    if (disconnected || process == null || !process.canTerminate()) {
                        done(false);
                        return;
                    }
                    if (!can_terminate_attached && !detached && process.isAttached()) {
                        process.detach(new IProcesses.DoneCommand() {
                            @Override
                            public void doneCommand(IToken token, Exception error) {
                                if (error != null) {
                                    error(error);
                                }
                                else {
                                    detached = true;
                                    run();
                                }
                            }
                        });
                        return;
                    }
                    if (!terminated) {
                        process.terminate(new IProcesses.DoneCommand() {
                            @Override
                            public void doneCommand(IToken token, Exception error) {
                                if (error != null) {
                                    error(error);
                                }
                                else {
                                    terminated = true;
                                    run();
                                }
                            }
                        });
                        return;
                    }
                    if (channel == null || shutdown) {
                        done(true);
                    }
                    else {
                        disconnect_wait_list.add(this);
                        closeChannel();
                    }
                }
            }.get();
        }
        catch (Exception x) {
            throw new DebugException(new TCFError(x));
        }
    }

    public boolean isExited() {
        return last_context_exited;
    }

    public boolean isProcessExited() {
        return process_exited;
    }

    public int getExitCode() {
        return process_exit_code;
    }

    public Collection<Map<String,Object>> getSignalList() {
        return process_signals;
    }

    public List<IPathMap.PathMapRule> getHostPathMap() {
        assert Protocol.isDispatchThread();
        return host_path_map;
    }

    public TCFDataCache<IPathMap.PathMapRule[]> getTargetPathMap() {
        assert Protocol.isDispatchThread();
        return target_path_map;
    }

    public Map<String,IStorage> getTargetPathMappingCache() {
        return target_path_mapping_cache;
    }

    public TCFDataCache<String[]> getContextQuery(final String query) {
        if (query == null) return null;
        TCFDataCache<String[]> cache = context_query_cache.get(query);
        if (cache == null) {
            if (disconnected) return null;
            final IContextQuery service = channel.getRemoteService(IContextQuery.class);
            if (service == null) return null;
            cache = new TCFDataCache<String[]>(channel) {
                @Override
                protected boolean startDataRetrieval() {
                    command = service.query(query, new IContextQuery.DoneQuery() {
                        public void doneQuery(IToken token, Exception error, String[] contexts) {
                            set(token, error, contexts);
                        }
                    });
                    return false;
                }
            };
            context_query_cache.put(query, cache);
        }
        return cache;
    }

    /**
     * Activate TCF launch: open communication channel and perform all necessary launch steps.
     * @param mode - on of launch mode constants defined in ILaunchManager.
     * @param id - TCF peer ID.
     */
    public void launchTCF(String mode, String id) {
        launchTCF(mode, id, null, null);
    }

    /**
     * Activate TCF launch: open communication channel and perform all necessary launch steps.
     * @param mode - on of launch mode constants defined in ILaunchManager.
     * @param id - TCF peer ID.
     * @param task - TCF task that is waiting until the launching is done, can be null
     * @param monitor - launching progress monitor, can be null
     */
    public void launchTCF(String mode, String id, TCFTask<Boolean> task, IProgressMonitor monitor) {
        assert Protocol.isDispatchThread();
        this.mode = mode;
        this.launch_task = task;
        this.launch_monitor = monitor;
        redirection_path.clear();
        if (id == null || id.length() == 0) {
            onDisconnected(new IOException("Invalid peer ID"));
            return;
        }
        for (;;) {
            int i = id.indexOf('/');
            if (i <= 0) {
                redirection_path.add(id);
                break;
            }
            redirection_path.add(id.substring(0, i));
            id = id.substring(i + 1);
        }
        Protocol.invokeLater(new Runnable() {
            final String id0 = redirection_path.removeFirst();
            int retry_cnt = 0;
            @Override
            public void run() {
                IPeer peer = Protocol.getLocator().getPeers().get(id0);
                if (peer == null) {
                    if (retry_cnt < 30) {
                        Protocol.invokeLater(1000, this);
                        retry_cnt++;
                    }
                    else {
                        onDisconnected(new Exception("Cannot locate peer " + id0));
                    }
                    return;
                }
                peer_name = getPeerName(peer);
                if (launch_monitor != null) launch_monitor.subTask("Connecting to " + peer_name);
                channel = peer.openChannel();
                channel.addChannelListener(new IChannel.IChannelListener() {

                    public void onChannelOpened() {
                        try {
                            peer_name = getPeerName(getPeer());
                            onConnected();
                        }
                        catch (Throwable x) {
                            channel.terminate(x);
                        }
                    }

                    public void congestionLevel(int level) {
                    }

                    public void onChannelClosed(Throwable error) {
                        channel.removeChannelListener(this);
                        onDisconnected(error);
                    }
                });
                assert channel.getState() == IChannel.STATE_OPENING;
            }
        });
        if (launch_monitor != null) launch_monitor.subTask("Searching for peers");
        connecting = true;
    }

    /**
     * Activate TCF launch: Re-use the passed in communication channel and perform all necessary launch steps.
     *
     * @param mode - on of launch mode constants defined in ILaunchManager.
     * @param peer_name - TCF peer name.
     * @param channel - TCF communication channel.
     */
    public void launchTCF(String mode, String peer_name, IChannel channel) {
        assert Protocol.isDispatchThread();
        this.mode = mode;
        this.redirection_path.clear();
        try {
            if (channel == null || channel.getRemotePeer() == null) throw new IOException("Invalid channel");
            this.peer_name = peer_name;
            this.channel = channel;

            IChannel.IChannelListener listener = new IChannel.IChannelListener() {

                public void onChannelOpened() {
                    try {
                        TCFLaunch.this.peer_name = getPeerName(getPeer());
                        onConnected();
                    }
                    catch (Throwable x) {
                        TCFLaunch.this.channel.terminate(x);
                    }
                }

                public void congestionLevel(int level) {
                }

                public void onChannelClosed(Throwable error) {
                    TCFLaunch.this.channel.removeChannelListener(this);
                    onDisconnected(error);
                }

            };
            channel.addChannelListener(listener);

            connecting = true;
            if (channel.getState() == IChannel.STATE_OPEN) {
                listener.onChannelOpened();
            }
            else if (channel.getState() != IChannel.STATE_OPENING) {
                throw new IOException("Channel is in invalid state");
            }
        }
        catch (Throwable e) {
            onDisconnected(e);
        }
    }

    /****************************************************************************************************************/

    private long getActionTimeStamp(String id) {
        Long l = context_action_timestamps.get(id);
        if (l == null) return 0;
        return l.longValue();
    }

    private void startAction(final String id) {
        if (active_actions.get(id) != null) return;
        LinkedList<TCFAction> list = context_action_queue.get(id);
        if (list == null || list.size() == 0) return;
        final TCFAction action = list.removeFirst();
        if (list.size() == 0) context_action_queue.remove(id);
        active_actions.put(id, action);
        final long timestamp = getActionTimeStamp(id);
        long time = System.currentTimeMillis();
        Protocol.invokeLater(timestamp + actions_interval - time, new Runnable() {
            public void run() {
                if (active_actions.get(id) != action) return;
                long time = System.currentTimeMillis();
                synchronized (pending_clients) {
                    if (pending_clients.size() > 0) {
                        if (time - timestamp < actions_interval + 1000) {
                            Protocol.invokeLater(20, this);
                            return;
                        }
                        pending_clients.clear();
                    }
                    else if (time < pending_clients_timestamp + 10) {
                        Protocol.invokeLater(pending_clients_timestamp + 10 - time, this);
                        return;
                    }
                }
                context_action_timestamps.put(id, time);
                for (ActionsListener l : action_listeners) l.onContextActionStart(action);
                action.run();
            }
        });
    }

    /**
     * Add an object to the set of pending clients.
     * Actions execution will be delayed until the set is empty,
     * but not longer then 1 second.
     * @param client
     */
    public void addPendingClient(Object client) {
        synchronized (pending_clients) {
            pending_clients.add(client);
            pending_clients_timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Remove an object from the set of pending clients.
     * Actions execution resumes when the set becomes empty.
     * @param client
     */
    public void removePendingClient(Object client) {
        synchronized (pending_clients) {
            if (pending_clients.remove(client) && pending_clients.size() == 0) {
                pending_clients_timestamp = System.currentTimeMillis();
            }
        }
    }

    /**
     * Set minimum interval between context actions execution.
     * @param interval - minimum interval in milliseconds.
     */
    public void setContextActionsInterval(long interval) {
        actions_interval = interval;
    }

    /**
     * Add a context action to actions queue.
     * Examples of context actions are resume/suspend/step commands,
     * which were requested by a user.
     * @param action
     */
    public void addContextAction(TCFAction action) {
        assert Protocol.isDispatchThread();
        String id = action.getContextID();
        LinkedList<TCFAction> list = context_action_queue.get(id);
        if (list == null) context_action_queue.put(id, list = new LinkedList<TCFAction>());
        int priority = action.getPriority();
        for (ListIterator<TCFAction> i = list.listIterator();;) {
            if (i.hasNext()) {
                if (priority <= i.next().getPriority()) continue;
                i.previous();
            }
            i.add(action);
            break;
        }
        startAction(id);
    }

    /**
     * Set action result for given context ID.
     * Action results are usually presented to a user same way as context suspend reasons.
     * @param id - debug context ID.
     * @param result - a string to be shown to user.
     */
    public void setContextActionResult(String id, String result) {
        assert Protocol.isDispatchThread();
        for (ActionsListener l : action_listeners) l.onContextActionResult(id, result);
    }

    /**
     * Remove an action from the queue.
     * The method should be called when the action execution is done.
     * @param action
     */
    public void removeContextAction(TCFAction action) {
        assert Protocol.isDispatchThread();
        String id = action.getContextID();
        assert active_actions.get(id) == action;
        active_actions.remove(id);
        for (ActionsListener l : action_listeners) l.onContextActionDone(action);
        startAction(id);
    }

    /**
     * Remove all actions from the queue of a debug context.
     * @param id - debug context ID.
     */
    public void removeContextActions(String id) {
        assert Protocol.isDispatchThread();
        context_action_queue.remove(id);
        context_action_timestamps.remove(id);
    }

    /**
     * Get action queue size of a debug context.
     * @param id - debug context ID.
     * @return count of pending actions.
     */
    public int getContextActionsCount(String id) {
        assert Protocol.isDispatchThread();
        LinkedList<TCFAction> list = context_action_queue.get(id);
        int n = list == null ? 0 : list.size();
        if (active_actions.get(id) != null) n++;
        return n;
    }

    /**
     * Add a listener that will be notified when an action execution is started or finished,
     * or when an action result is posted.
     * @param l - action listener.
     */
    public void addActionsListener(ActionsListener l) {
        action_listeners.add(l);
    }

    /**
     * Remove an action listener that was registered with addActionsListener().
     * @param l - action listener.
     */
    public void removeActionsListener(ActionsListener l) {
        action_listeners.remove(l);
    }

    /**
     * Get context filer of the launch.
     * By default, TCF debugger shows all remote debug contexts.
     * Context filter is used to hide unwanted contexts to reduce UI clutter.
     * @return context filter.
     */
    public Set<String> getContextFilter() {
        return context_filter;
    }
}
