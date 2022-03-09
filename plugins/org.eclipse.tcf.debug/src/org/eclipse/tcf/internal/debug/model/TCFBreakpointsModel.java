/*******************************************************************************
 * Copyright (c) 2007-2022 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.tcf.internal.debug.Activator;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IBreakpoints;

/**
 * TCFBreakpointsModel class handles breakpoints for all active TCF launches.
 * It downloads initial set of breakpoint data when launch is activated,
 * listens for Eclipse breakpoint manager events and propagates breakpoint changes to TCF targets.
 */
public class TCFBreakpointsModel {

    public static final String
        CDATA_CLIENT_ID    = "ClientID",
        CDATA_TYPE         = "Type",
        CDATA_FILE         = "File",
        CDATA_MARKER       = "Marker";

    public static final String
        ATTR_ID            = ITCFConstants.ID_TCF_DEBUG_MODEL + '.' + IBreakpoints.PROP_ID,
        ATTR_STATUS        = ITCFConstants.ID_TCF_DEBUG_MODEL + '.' + "Status",
        ATTR_MESSAGE       = IMarker.MESSAGE,
        ATTR_ENABLED       = IBreakpoint.ENABLED,
        ATTR_INSTALL_COUNT = "org.eclipse.cdt.debug.core.installCount",
        ATTR_ADDRESS       = "org.eclipse.cdt.debug.core.address",
        ATTR_FUNCTION      = "org.eclipse.cdt.debug.core.function",
        ATTR_EXPRESSION    = "org.eclipse.cdt.debug.core.expression",
        ATTR_READ          = "org.eclipse.cdt.debug.core.read",
        ATTR_WRITE         = "org.eclipse.cdt.debug.core.write",
        ATTR_SIZE          = "org.eclipse.cdt.debug.core.range",
        ATTR_FILE          = "org.eclipse.cdt.debug.core.sourceHandle",
        ATTR_LINE          = IMarker.LINE_NUMBER,
        ATTR_CHAR          = IMarker.CHAR_START,
        ATTR_REQESTED_FILE = "requestedSourceHandle",
        ATTR_REQESTED_LINE = "requestedLine",
        ATTR_REQESTED_CHAR = "requestedCharStart",
        ATTR_CONDITION     = "org.eclipse.cdt.debug.core.condition",
        ATTR_PRINTF_STRING = "org.eclipse.cdt.debug.core.printf_string",
        ATTR_IGNORE_COUNT  = "org.eclipse.cdt.debug.core.ignoreCount",
        ATTR_CONTEXTNAMES  = ITCFConstants.ID_TCF_DEBUG_MODEL + '.' + IBreakpoints.PROP_CONTEXT_NAMES,
        ATTR_CONTEXTIDS    = ITCFConstants.ID_TCF_DEBUG_MODEL + '.' + IBreakpoints.PROP_CONTEXT_IDS,
        ATTR_EXE_PATHS     = ITCFConstants.ID_TCF_DEBUG_MODEL + '.' + IBreakpoints.PROP_EXECUTABLE_PATHS,
        ATTR_STOP_GROUP    = ITCFConstants.ID_TCF_DEBUG_MODEL + '.' + IBreakpoints.PROP_STOP_GROUP,
        ATTR_CONTEXT_QUERY = ITCFConstants.ID_TCF_DEBUG_MODEL + '.' + IBreakpoints.PROP_CONTEXT_QUERY,
        ATTR_LINE_OFFSET   = ITCFConstants.ID_TCF_DEBUG_MODEL + '.' + IBreakpoints.PROP_LINE_OFFSET,
        ATTR_SKIP_PROLOGUE = ITCFConstants.ID_TCF_DEBUG_MODEL + '.' + IBreakpoints.PROP_SKIP_PROLOGUE,
        ATTR_CT_INP        = ITCFConstants.ID_TCF_DEBUG_MODEL + '.' + IBreakpoints.PROP_CT_INP,
        ATTR_CT_OUT        = ITCFConstants.ID_TCF_DEBUG_MODEL + '.' + IBreakpoints.PROP_CT_OUT,
        ATTR_EVENT_TYPE    = "org.eclipse.cdt.debug.core.eventbreakpoint_event_id",
        ATTR_EVENT_ARGS    = "org.eclipse.cdt.debug.core.eventbreakpoint_event_arg",
        ATTR_TYPE          = "org.eclipse.cdt.debug.core.breakpointType",
        ATTR_TCF_STAMP     = "org.eclipse.tcf.debug.tcfStamp";

    public static final int
        ATTR_TYPE_TEMPORARY = 0x1,
        ATTR_TYPE_REGULAR = 0x0 << 1,
        ATTR_TYPE_HARDWARE = 0x1 << 1,
        ATTR_TYPE_SOFTWARE = 0x2 << 1;

    private final IBreakpointManager bp_manager = DebugPlugin.getDefault().getBreakpointManager();
    private final HashMap<IChannel,Map<String,Object>> channels = new HashMap<IChannel,Map<String,Object>>();
    private final HashMap<String,IBreakpoint> id2bp = new HashMap<String,IBreakpoint>();

    private abstract class BreakpointUpdate implements Runnable {

        private final IBreakpoint breakpoint;
        private boolean removed;
        protected final Map<String,Object> marker_attrs;
        protected final boolean is_local;
        protected final String marker_id;
        private final String marker_file;
        private final String marker_type;
        private boolean exec_done;

        IBreakpoints service;
        IBreakpoints.DoneCommand done;
        Map<String,Object> tcf_attrs;

        BreakpointUpdate(IBreakpoint breakpoint, boolean removed) throws CoreException, IOException {
            this.breakpoint = breakpoint;
            this.removed = removed;
            IMarker marker = breakpoint.getMarker();
            marker_attrs = new HashMap<String,Object>(marker.getAttributes());
            is_local = isLocal(marker);
            marker_file = getFilePath(breakpoint.getMarker().getResource());
            marker_type = breakpoint.getMarker().getType();
            marker_id = getBreakpointID(breakpoint);
        }

        synchronized void exec() throws InterruptedException {
            assert !exec_done;
            assert !Protocol.isDispatchThread();
            if (marker_id != null) {
                Protocol.invokeLater(this);
                while (!exec_done) wait();
            }
        }

        private synchronized void exec_done() {
            exec_done = true;
            BreakpointUpdate.this.notify();
        }

        @Override
        public void run() {
            if (disposed) {
                exec_done();
                return;
            }
            if (removed) id2bp.remove(marker_id);
            else id2bp.put(marker_id, breakpoint);
            if (is_local) {
                for (final IChannel channel : channels.keySet()) {
                    tcf_attrs = toBreakpointAttributes(channel, marker_id, marker_file, marker_type, marker_attrs);
                    service = channel.getRemoteService(IBreakpoints.class);
                    if (!isSupported(channel, breakpoint)) continue;
                    done = new IBreakpoints.DoneCommand() {
                        public void doneCommand(IToken token, Exception error) {
                            if (error != null) channel.terminate(error);
                        }
                    };
                    update();
                }
            }
            Protocol.sync(new Runnable() {
                public void run() {
                    exec_done();
                }
            });
        };

        abstract void update();
    }

    private final IBreakpointListener breakpoint_listener = new IBreakpointListener() {

        public void breakpointAdded(final IBreakpoint breakpoint) {
            try {
                new BreakpointUpdate(breakpoint, false) {
                    @Override
                    void update() {
                        service.add(tcf_attrs, done);
                    }
                }.exec();
            }
            catch (Throwable x) {
                Activator.log("Unhandled exception in breakpoint listener", x);
            }
        }

        private Set<String> calcMarkerDeltaKeys(IMarker marker, IMarkerDelta delta) throws CoreException {
            Set<String> keys = new HashSet<String>();
            if (delta == null) return keys;
            Map<String,Object> m0 = delta.getAttributes();
            Map<String,Object> m1 = marker.getAttributes();
            if (m0 != null) keys.addAll(m0.keySet());
            if (m1 != null) keys.addAll(m1.keySet());
            for (Iterator<String> i = keys.iterator(); i.hasNext();) {
                String key = i.next();
                Object v0 = m0 != null ? m0.get(key) : null;
                Object v1 = m1 != null ? m1.get(key) : null;
                if (v0 instanceof String && ((String)v0).length() == 0) v0 = null;
                if (v1 instanceof String && ((String)v1).length() == 0) v1 = null;
                if (v0 instanceof Boolean && !((Boolean)v0).booleanValue()) v0 = null;
                if (v1 instanceof Boolean && !((Boolean)v1).booleanValue()) v1 = null;
                if ((v0 == null) != (v1 == null)) continue;
                if (v0 != null && !v0.equals(v1)) continue;
                i.remove();
            }
            if (marker.getAttribute(ATTR_REQESTED_FILE, "").length() > 0) keys.remove(ATTR_FILE);
            if (marker.getAttribute(ATTR_REQESTED_LINE, -1) >= 0) keys.remove(ATTR_LINE);
            if (marker.getAttribute(ATTR_REQESTED_CHAR, -1) >= 0) keys.remove(ATTR_CHAR);
            keys.remove(ATTR_INSTALL_COUNT);
            keys.remove(ATTR_TCF_STAMP);
            keys.remove(ATTR_MESSAGE);
            keys.remove(ATTR_STATUS);
            return keys;
        }

        public void breakpointChanged(final IBreakpoint breakpoint, IMarkerDelta delta) {
            try {
                final Set<String> s = calcMarkerDeltaKeys(breakpoint.getMarker(), delta);
                if (s.isEmpty()) return;
                new BreakpointUpdate(breakpoint, false) {
                    @Override
                    void update() {
                        if (s.size() == 1 && s.contains(ATTR_ENABLED)) {
                            Boolean enabled = (Boolean)tcf_attrs.get(IBreakpoints.PROP_ENABLED);
                            if (enabled == null || !enabled.booleanValue()) {
                                service.disable(new String[]{ marker_id }, done);
                            }
                            else {
                                service.enable(new String[]{ marker_id }, done);
                            }
                        }
                        else {
                            service.change(tcf_attrs, done);
                        }
                    }
                }.exec();
            }
            catch (Throwable x) {
                Activator.log("Unhandled exception in breakpoint listener", x);
            }
        }

        public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
            try {
                new BreakpointUpdate(breakpoint, true) {
                    @Override
                    void update() {
                        service.remove(new String[]{ marker_id }, done);
                    }
                }.exec();
            }
            catch (Throwable x) {
                Activator.log("Unhandled exception in breakpoint listener", x);
            }
        }
    };

    private final IBreakpointManagerListener manager_listener = new IBreakpointManagerListener() {

        public void breakpointManagerEnablementChanged(final boolean enabled) {
            try {
                IBreakpoint[] arr = bp_manager.getBreakpoints();
                if (arr == null || arr.length == 0) return;
                final Map<String,IBreakpoint> map = new HashMap<String,IBreakpoint>();
                for (int i = 0; i < arr.length; i++) {
                    IMarker marker = arr[i].getMarker();
                    Boolean b = marker.getAttribute(ATTR_ENABLED, Boolean.FALSE);
                    if (!b.booleanValue()) continue;
                    String id = getBreakpointID(arr[i]);
                    if (id == null) continue;
                    map.put(id, arr[i]);
                }
                if (map.isEmpty()) return;
                Runnable r = new Runnable() {
                    public void run() {
                        if (disposed) {
                            synchronized (map) {
                                map.clear();
                                map.notify();
                                return;
                            }
                        }
                        for (final IChannel channel : channels.keySet()) {
                            IBreakpoints service = channel.getRemoteService(IBreakpoints.class);
                            Set<String> ids = new HashSet<String>();
                            for (String id : map.keySet()) {
                                IBreakpoint bp = map.get(id);
                                if (isSupported(channel, bp)) ids.add(id);
                            }
                            IBreakpoints.DoneCommand done = new IBreakpoints.DoneCommand() {
                                public void doneCommand(IToken token, Exception error) {
                                    if (error != null) channel.terminate(error);
                                }
                            };
                            if (enabled) {
                                service.enable(ids.toArray(new String[ids.size()]), done);
                            }
                            else {
                                service.disable(ids.toArray(new String[ids.size()]), done);
                            }
                        }
                        Protocol.sync(new Runnable() {
                            public void run() {
                                synchronized (map) {
                                    map.clear();
                                    map.notify();
                                }
                            }
                        });
                    }
                };
                synchronized (map) {
                    assert !Protocol.isDispatchThread();
                    Protocol.invokeLater(r);
                    while (!map.isEmpty()) map.wait();
                }
            }
            catch (Throwable x) {
                Activator.log("Unhandled exception in breakpoint listener", x);
            }
        }
    };

    private boolean disposed;

    public static TCFBreakpointsModel getBreakpointsModel() {
        return Activator.getBreakpointsModel();
    }

    public void dispose() {
        bp_manager.removeBreakpointListener(breakpoint_listener);
        bp_manager.removeBreakpointManagerListener(manager_listener);
        channels.clear();
        disposed = true;
    }

    public boolean isSupported(IChannel channel, IBreakpoint bp) {
        // TODO: implement per-channel breakpoint filtering
        return true;
    }

    /**
     * Get TCF ID of a breakpoint.
     * @param bp - IBreakpoint object.
     * @return TCF ID of the breakpoint.
     * @throws CoreException
     */
    public static String getBreakpointID(IBreakpoint bp) throws CoreException {
        IMarker marker = bp.getMarker();
        String id = (String)marker.getAttributes().get(ATTR_ID);
        if (id != null) return id;
        id = marker.getResource().getLocationURI().toString();
        return id + ':' + marker.getId();
    }

    /**
     * Get IBreakpoint for given TCF breakpoint ID.
     * The mapping works only for breakpoints that were sent to (or received from) a debug target.
     * It can be used to map target responses to IBreakpoint objects.
     * @param id - TCF breakpoint ID.
     * @return IBreakpoint object associated with the ID, or null.
     */
    public IBreakpoint getBreakpoint(String id) {
        assert Protocol.isDispatchThread();
        return id2bp.get(id);
    }

    /**
     * Check breakpoint ownership.
     * @param properties - breakpoint properties as reported by TCF Breakpoints service.
     * @return true if the breakpoint is owned by local instance of Eclipse.
     * Return false if the properties represent a breakpoint created by some other TCF client.
     */
    @SuppressWarnings("unchecked")
    public static boolean isLocal(Map<String,Object> properties) {
        if (properties == null) return false;
        Map<String,Object> client_data = (Map<String,Object>)properties.get(IBreakpoints.PROP_CLIENT_DATA);
        if (client_data == null) return false;
        String id = (String)client_data.get(TCFBreakpointsModel.CDATA_CLIENT_ID);
        return Activator.getClientID().equals(id);
    }

    /**
     * Check breakpoint marker ownership.
     * @param marker - breakpoint marker.
     * @return true if the marker is owned by local instance of Eclipse.
     * Return false if the marker represents a breakpoint created by some other TCF client.
     */
    public static boolean isLocal(IMarker marker) {
        try {
            Map<String,Object> marker_attrs = marker.getAttributes();
            if (marker_attrs == null) return false;
            return !Boolean.FALSE.equals(marker_attrs.get(IBreakpoint.PERSISTED));
        }
        catch (CoreException e) {
            return false;
        }
    }

    /**
     * Download breakpoint info to a target.
     * This is supposed to be done once after a channel is opened.
     * @param channel - TCF communication channel.
     * @param done - client callback.
     * @throws IOException
     * @throws CoreException
     */
    @SuppressWarnings("unchecked")
    public void downloadBreakpoints(final IChannel channel, final Runnable done) throws IOException, CoreException {
        assert !disposed;
        assert Protocol.isDispatchThread();
        final IBreakpoints service = channel.getRemoteService(IBreakpoints.class);
        if (service == null) {
            Protocol.invokeLater(done);
            return;
        }
        service.getCapabilities(null, new IBreakpoints.DoneGetCapabilities() {
            public void doneGetCapabilities(IToken token, Exception error, Map<String,Object> capabilities) {
                if (channel.getState() != IChannel.STATE_OPEN) {
                    Protocol.invokeLater(done);
                    return;
                }
                if (channels.isEmpty()) {
                    bp_manager.addBreakpointListener(breakpoint_listener);
                    bp_manager.addBreakpointManagerListener(manager_listener);
                }
                channel.addChannelListener(new IChannel.IChannelListener() {
                    public void congestionLevel(int level) {
                    }
                    public void onChannelClosed(Throwable error) {
                        if (disposed) return;
                        channels.remove(channel);
                        if (channels.isEmpty()) {
                            bp_manager.removeBreakpointListener(breakpoint_listener);
                            bp_manager.removeBreakpointManagerListener(manager_listener);
                            id2bp.clear();
                        }
                    }
                    public void onChannelOpened() {
                    }
                });
                channels.put(channel, capabilities);
                IBreakpoint[] arr = bp_manager.getBreakpoints();
                if (arr != null && arr.length > 0) {
                    List<Map<String,Object>> bps = new ArrayList<Map<String,Object>>(arr.length);
                    for (int i = 0; i < arr.length; i++) {
                        try {
                            if (!isSupported(channel, arr[i])) continue;
                            String id = getBreakpointID(arr[i]);
                            if (id == null) continue;
                            if (!arr[i].isPersisted()) continue;
                            IMarker marker = arr[i].getMarker();
                            String file = getFilePath(marker.getResource());
                            bps.add(toBreakpointAttributes(channel, id, file, marker.getType(), marker.getAttributes()));
                            id2bp.put(id, arr[i]);
                        }
                        catch (Exception x) {
                            Activator.log("Cannot get breakpoint attributes", x);
                        }
                    }
                    if (!bps.isEmpty()) {
                        Map<String, Object>[] bp_arr = (Map<String,Object>[])bps.toArray(new Map[bps.size()]);
                        service.set(bp_arr, new IBreakpoints.DoneCommand() {
                            public void doneCommand(IToken token, Exception error) {
                                if (error == null) done.run();
                                else channel.terminate(error);
                            }
                        });
                        return;
                    }
                }
                Protocol.invokeLater(done);
            }
        });
    }

    private String getFilePath(IResource resource) throws IOException {
        if (resource == ResourcesPlugin.getWorkspace().getRoot()) return null;
        IPath p = resource.getRawLocation();
        if (p == null) return null;
        return p.toFile().getCanonicalPath();
    }

    /**
     * Translate TCF breakpoint properties to Eclipse breakpoint marker attributes.
     * @param p - TCF breakpoint properties.
     * @return Eclipse marker attributes.
     */
    @SuppressWarnings("unchecked")
    public Map<String,Object> toMarkerAttributes(Map<String,Object> p) {
        assert !disposed;
        assert Protocol.isDispatchThread();
        Map<String,Object> client_data = (Map<String,Object>)p.get(IBreakpoints.PROP_CLIENT_DATA);
        if (client_data != null) {
            Map<String,Object> m = (Map<String,Object>)client_data.get(CDATA_MARKER);
            if (m != null) {
                m = new HashMap<String,Object>(m);
                m.put(ATTR_ID, p.get(IBreakpoints.PROP_ID));
                Boolean enabled = (Boolean)p.get(IBreakpoints.PROP_ENABLED);
                m.put(ATTR_ENABLED, enabled == null ? Boolean.FALSE : enabled);
                return m;
            }
        }
        Map<String,Object> m = new HashMap<String,Object>();
        for (Map.Entry<String,Object> e : p.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            if (key.equals(IBreakpoints.PROP_ENABLED)) continue;
            if (key.equals(IBreakpoints.PROP_FILE)) continue;
            if (key.equals(IBreakpoints.PROP_LINE)) continue;
            if (key.equals(IBreakpoints.PROP_COLUMN)) continue;
            if (key.equals(IBreakpoints.PROP_LOCATION)) continue;
            if (key.equals(IBreakpoints.PROP_ACCESS_MODE)) continue;
            if (key.equals(IBreakpoints.PROP_SIZE)) continue;
            if (key.equals(IBreakpoints.PROP_CONDITION)) continue;
            if (key.equals(IBreakpoints.PROP_EVENT_TYPE)) continue;
            if (key.equals(IBreakpoints.PROP_EVENT_ARGS)) continue;
            if (IBreakpoints.PROP_CONTEXT_IDS.equals(key) ||
                    IBreakpoints.PROP_CONTEXT_NAMES.equals(key) ||
                    IBreakpoints.PROP_STOP_GROUP.equals(key) ||
                    IBreakpoints.PROP_EXECUTABLE_PATHS.equals(key)) {
                if (val instanceof String[]) {
                    StringBuffer bf = new StringBuffer();
                    for (String s : (String[])val) {
                        if (bf.length() > 0) bf.append(',');
                        bf.append(s);
                    }
                    if (bf.length() == 0) continue;
                    val = bf.toString();
                }
                else if (val instanceof Collection) {
                    StringBuffer bf = new StringBuffer();
                    for (String s : (Collection<String>)val) {
                        if (bf.length() > 0) bf.append(',');
                        bf.append(s);
                    }
                    if (bf.length() == 0) continue;
                    val = bf.toString();
                }
            }
            if (!(val instanceof String) && !(val instanceof Boolean) && !(val instanceof Integer)) {
                // class of the value is not supported by Marker, convert to JSON string
                try {
                    val = "JSON:" + JSON.toJSON(val);
                }
                catch (IOException x) {
                    continue;
                }
            }
            m.put(ITCFConstants.ID_TCF_DEBUG_MODEL + '.' + key, val);
        }
        Boolean enabled = (Boolean)p.get(IBreakpoints.PROP_ENABLED);
        m.put(ATTR_ENABLED, enabled == null ? Boolean.FALSE : enabled);
        String location = (String)p.get(IBreakpoints.PROP_LOCATION);
        if (location != null && location.length() > 0) {
            int access_mode = IBreakpoints.ACCESSMODE_EXECUTE;
            Number access_mode_num = (Number)p.get(IBreakpoints.PROP_ACCESS_MODE);
            if (access_mode_num != null) access_mode = access_mode_num.intValue();
            if ((access_mode & IBreakpoints.ACCESSMODE_EXECUTE) != 0) {
                if (Character.isDigit(location.charAt(0))) {
                    m.put(ATTR_ADDRESS, location);
                }
                else {
                    m.put(ATTR_FUNCTION, location);
                }
            }
            else {
                m.put(ATTR_EXPRESSION, location.replaceFirst("^&\\((.+)\\)$", "$1"));
                m.put(ATTR_READ, (access_mode & IBreakpoints.ACCESSMODE_READ) != 0);
                m.put(ATTR_WRITE, (access_mode & IBreakpoints.ACCESSMODE_WRITE) != 0);
            }
            Number size_num = (Number)p.get(IBreakpoints.PROP_SIZE);
            if (size_num != null) m.put(ATTR_SIZE, size_num.toString());
        }
        m.put(IBreakpoint.REGISTERED, Boolean.TRUE);
        m.put(IBreakpoint.PERSISTED, Boolean.TRUE);
        m.put(IBreakpoint.ID, ITCFConstants.ID_TCF_DEBUG_MODEL);
        String file = (String)p.get(IBreakpoints.PROP_FILE);
        if (file != null && file.length() > 0) {
            m.put(ATTR_FILE, file);
        }
        Number line = (Number)p.get(IBreakpoints.PROP_LINE);
        if (line != null) {
            m.put(ATTR_LINE, Integer.valueOf(line.intValue()));
            Number column = (Number)p.get(IBreakpoints.PROP_COLUMN);
            if (column != null) {
                m.put(IMarker.CHAR_START, new Integer(column.intValue()));
                m.put(IMarker.CHAR_END, new Integer(column.intValue() + 1));
            }
        }
        String condition = (String)p.get(IBreakpoints.PROP_CONDITION);
        if (condition != null && condition.length() > 0) m.put(ATTR_CONDITION, condition);
        String event_type = (String)p.get(IBreakpoints.PROP_EVENT_TYPE);
        if (event_type != null && event_type.length() > 0) m.put(ATTR_EVENT_TYPE, event_type);
        String event_args = (String)p.get(IBreakpoints.PROP_EVENT_ARGS);
        if (event_args != null && event_args.length() > 0) m.put(ATTR_EVENT_ARGS, event_args);
        Number ignore_count = (Number)p.get(IBreakpoints.PROP_IGNORE_COUNT);
        if (ignore_count != null) m.put(ATTR_IGNORE_COUNT, ignore_count);
        Boolean temporary = (Boolean)p.get(IBreakpoints.PROP_TEMPORARY);
        if (temporary != null && temporary.booleanValue()) {
            Integer cdt_type = (Integer)m.get(ATTR_TYPE);
            cdt_type = cdt_type != null ? cdt_type : 0;
            cdt_type = cdt_type | ATTR_TYPE_TEMPORARY;
            m.put(ATTR_TYPE, cdt_type);
        }
        String type = (String)p.get(IBreakpoints.PROP_TYPE);
        if (type != null) {
            Integer cdt_type = (Integer)m.get(ATTR_TYPE);
            cdt_type = cdt_type != null ? cdt_type : 0;
            if (IBreakpoints.TYPE_HARDWARE.equals(type)) {
                cdt_type = cdt_type | ATTR_TYPE_HARDWARE;
            }
            else if (IBreakpoints.TYPE_SOFTWARE.equals(type)) {
                cdt_type = cdt_type | ATTR_TYPE_SOFTWARE;
            }
            m.put(ATTR_TYPE, cdt_type);
        }
        String msg = null;
        if (location != null) msg = location;
        else if (file != null && line != null) msg = file + ":" + line;
        else msg = (String)p.get(IBreakpoints.PROP_ID);
        m.put(ATTR_MESSAGE, "Breakpoint: " + msg);
        m.put(ATTR_TCF_STAMP, Boolean.TRUE.toString());

        return m;
    }

    /**
     * Translate Eclipse breakpoint marker attributes to TCF breakpoint properties.
     * @param channel - TCF communication channel.
     * @param id - breakpoint ID.
     * @param file - the maker file or null.
     * @param type - the marker type.
     * @param p - the marker attributes.
     * @return TCF breakpoint properties.
     */
    public Map<String,Object> toBreakpointAttributes(IChannel channel, String id,
            String marker_file, String marker_type, Map<String,Object> p) {
        assert !disposed;
        assert Protocol.isDispatchThread();
        Map<String,Object> m = new HashMap<String,Object>();
        Map<String,Object> capabilities = channels.get(channel);
        Map<String,Object> client_data = null;
        if (capabilities != null) {
            Object obj = capabilities.get(IBreakpoints.CAPABILITY_CLIENT_DATA);
            if (obj instanceof Boolean && ((Boolean)obj).booleanValue()) client_data = new HashMap<String,Object>();
        }
        m.put(IBreakpoints.PROP_ID, id);
        if (client_data != null) {
            m.put(IBreakpoints.PROP_CLIENT_DATA, client_data);
            client_data.put(CDATA_CLIENT_ID, Activator.getClientID());
            if (marker_type != null) client_data.put(CDATA_TYPE, marker_type);
            if (marker_file != null) client_data.put(CDATA_FILE, marker_file);
            Map<String,Object> x = new HashMap<String,Object>(p);
            x.remove(ATTR_INSTALL_COUNT);
            x.remove(ATTR_TCF_STAMP);
            x.remove(ATTR_STATUS);
            client_data.put(CDATA_MARKER, x);
        }
        for (Map.Entry<String,Object> e : p.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            if (key.equals(ATTR_STATUS)) continue;
            if (key.equals(ATTR_TCF_STAMP)) continue;
            if (key.startsWith(ITCFConstants.ID_TCF_DEBUG_MODEL)) {
                String tcf_key = key.substring(ITCFConstants.ID_TCF_DEBUG_MODEL.length() + 1);
                if (val instanceof String) {
                    String str = (String)val;
                    if (IBreakpoints.PROP_CONTEXT_IDS.equals(tcf_key)) {
                        if (str.length() == 0) continue;
                        val = filterContextIds(channel, str.split(",\\s*"));
                    }
                    else if (IBreakpoints.PROP_CONTEXT_QUERY.equals(tcf_key)) {
                        if (str.length() == 0) continue;
                    }
                    else if (IBreakpoints.PROP_CONTEXT_NAMES.equals(tcf_key) ||
                            IBreakpoints.PROP_STOP_GROUP.equals(tcf_key) ||
                            IBreakpoints.PROP_EXECUTABLE_PATHS.equals(tcf_key)) {
                        val = str.split(",\\s*");
                    }
                    else if (str.startsWith("JSON:")) {
                        try {
                            val = JSON.parseOne(str.substring(5).getBytes("UTF-8"));
                        }
                        catch (Exception x) {
                            // Not JSON, keep value as String
                        }
                    }
                }
                m.put(tcf_key, val);
            }
        }
        Boolean enabled = (Boolean)p.get(ATTR_ENABLED);
        if (enabled != null && enabled.booleanValue() && bp_manager.isEnabled()) {
            m.put(IBreakpoints.PROP_ENABLED, enabled);
        }
        String file = (String)p.get(ATTR_REQESTED_FILE);
        if (file == null || file.length() == 0) file = (String)p.get(ATTR_FILE);
        if (file == null || file.length() == 0) file = marker_file;
        if (file != null && file.trim().length() > 0) {
            String name = file;
            boolean file_mapping = false;
            if (capabilities != null) {
                Object obj = capabilities.get(IBreakpoints.CAPABILITY_FILE_MAPPING);
                if (obj instanceof Boolean) file_mapping = ((Boolean)obj).booleanValue();
            }
            if (!file_mapping) {
                int i = file.lastIndexOf('/');
                int j = file.lastIndexOf('\\');
                if (i > j) name = file.substring(i + 1);
                else if (i < j) name = file.substring(j + 1);
            }
            m.put(IBreakpoints.PROP_FILE, name);
            Integer line = (Integer)p.get(ATTR_REQESTED_LINE);
            if (line == null || line < 0) line = (Integer)p.get(ATTR_LINE);
            if (line != null && line >= 0) {
                m.put(IBreakpoints.PROP_LINE, Integer.valueOf(line.intValue()));
                Integer column = (Integer)p.get(ATTR_REQESTED_CHAR);
                if (column == null || column < 0) column = (Integer)p.get(ATTR_CHAR);
                if (column != null && column >= 0) m.put(IBreakpoints.PROP_COLUMN, column);
            }
        }
        if (p.get(ATTR_EXPRESSION) != null) {
            String expr = (String)p.get(ATTR_EXPRESSION);
            if (expr != null && expr.length() != 0) {
                boolean writeAccess = Boolean.TRUE.equals(p.get(ATTR_WRITE));
                boolean readAccess = Boolean.TRUE.equals(p.get(ATTR_READ));
                int accessMode = 0;
                if (readAccess) accessMode |= IBreakpoints.ACCESSMODE_READ;
                if (writeAccess) accessMode |= IBreakpoints.ACCESSMODE_WRITE;
                m.put(IBreakpoints.PROP_ACCESS_MODE, Integer.valueOf(accessMode));
                Object range = p.get(ATTR_SIZE);
                if (range != null) {
                    int size = Integer.parseInt(range.toString());
                    if (size > 0) m.put(IBreakpoints.PROP_SIZE, size);
                }
                if (!Character.isDigit(expr.charAt(0))) {
                    expr = "&(" + expr + ')';
                }
                m.put(IBreakpoints.PROP_LOCATION, expr);
                if (m.get(IBreakpoints.PROP_FILE) != null && m.get(IBreakpoints.PROP_LINE) == null) {
                    m.put(IBreakpoints.PROP_LINE, Integer.valueOf(1));
                }
            }
        }
        else if (p.get(ATTR_FUNCTION) != null) {
            String expr = (String)p.get(ATTR_FUNCTION);
            if (expr != null && expr.length() != 0) {
                // Strip function parameters
                int paren = expr.indexOf('(');
                if (paren > 0)
                    expr = expr.substring(0, paren);
                m.put(IBreakpoints.PROP_LOCATION, expr);
            }
            if (!m.containsKey(IBreakpoints.PROP_SKIP_PROLOGUE) && capabilities != null) {
                Object obj = capabilities.get(IBreakpoints.CAPABILITY_SKIP_PROLOGUE);
                if (obj instanceof Boolean && ((Boolean)obj).booleanValue()) {
                    m.put(IBreakpoints.PROP_SKIP_PROLOGUE, true);
                }
            }
        }
        else if (file == null) {
            String address = (String)p.get(ATTR_ADDRESS);
            if (address != null && address.length() > 0) m.put(IBreakpoints.PROP_LOCATION, address);
        }
        String condition = (String)p.get(ATTR_CONDITION);
        String printf_string = (String)p.get(ATTR_PRINTF_STRING);
        if (printf_string != null && printf_string.length() > 0)
            if (condition != null && condition.length() > 0)
                condition = '(' + condition + ") && " + "$printf(" + printf_string + ')';
            else
                condition = "$printf(" + printf_string + ')';
        if (condition != null && condition.length() > 0) m.put(IBreakpoints.PROP_CONDITION, condition);
        String event_type = (String)p.get(ATTR_EVENT_TYPE);
        if (event_type != null && event_type.length() > 0) m.put(IBreakpoints.PROP_EVENT_TYPE, event_type);
        String event_args = (String)p.get(ATTR_EVENT_ARGS);
        if (event_args != null && event_args.length() > 0) m.put(IBreakpoints.PROP_EVENT_ARGS, event_args);
        Number ignore_count = (Number)p.get(ATTR_IGNORE_COUNT);
        if (ignore_count != null && ignore_count.intValue() > 0) m.put(IBreakpoints.PROP_IGNORE_COUNT, ignore_count);
        Integer cdt_type = (Integer)p.get(ATTR_TYPE);
        if (cdt_type != null) {
            if ((cdt_type.intValue() & ATTR_TYPE_TEMPORARY) != 0) {
                m.put(IBreakpoints.PROP_TEMPORARY, true);
            }
            if ((cdt_type.intValue() & ATTR_TYPE_HARDWARE) != 0) {
                m.put(IBreakpoints.PROP_TYPE, IBreakpoints.TYPE_HARDWARE);
            }
            else if ((cdt_type.intValue() & ATTR_TYPE_SOFTWARE) != 0) {
                m.put(IBreakpoints.PROP_TYPE, IBreakpoints.TYPE_SOFTWARE);
            }
            else  {
                m.put(IBreakpoints.PROP_TYPE, IBreakpoints.TYPE_AUTO);
            }
        }
        return m;
    }

    /**
     * Filter given array of scope IDs of the form sessionId/contextId
     * to those applicable to the given channel.
     */
    private String[] filterContextIds(IChannel channel, String[] scopeIds) {
        String sessionId = getSessionId(channel);
        List<String> contextIds = new ArrayList<String>();
        for (String scopeId : scopeIds) {
            if (scopeId.length() == 0) continue;
            int slash = scopeId.indexOf('/');
            if (slash < 0) {
                contextIds.add(scopeId);
            }
            else if (sessionId != null && sessionId.equals(scopeId.substring(0, slash))) {
                contextIds.add(scopeId.substring(slash+1));
            }
        }
        return (String[]) contextIds.toArray(new String[contextIds.size()]);
    }

    /**
     * @return launch config name for given channel or <code>null</code>
     */
    private String getSessionId(IChannel channel) {
        ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
        for (ILaunch launch : launches) {
            if (launch instanceof TCFLaunch) {
                if (channel == ((TCFLaunch) launch).getChannel()) {
                    ILaunchConfiguration lc = launch.getLaunchConfiguration();
                    return lc != null ? lc.getName() : null;
                }
            }
        }
        return null;
    }
}
