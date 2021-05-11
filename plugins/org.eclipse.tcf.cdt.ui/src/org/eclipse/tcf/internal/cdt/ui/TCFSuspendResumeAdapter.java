/*******************************************************************************
 * Copyright (c) 2010, 2017 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.cdt.core.IAddress;
import org.eclipse.cdt.debug.core.model.IMoveToAddress;
import org.eclipse.cdt.debug.core.model.IMoveToLine;
import org.eclipse.cdt.debug.core.model.IResumeAtAddress;
import org.eclipse.cdt.debug.core.model.IResumeAtLine;
import org.eclipse.cdt.debug.core.model.IRunToAddress;
import org.eclipse.cdt.debug.core.model.IRunToLine;
import org.eclipse.cdt.debug.internal.core.sourcelookup.CSourceLookupDirector;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.tcf.internal.debug.actions.TCFAction;
import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.internal.debug.ui.model.TCFChildren;
import org.eclipse.tcf.internal.debug.ui.model.TCFDebugTask;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeRegister;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.services.IBreakpoints;
import org.eclipse.tcf.services.ILineNumbers;
import org.eclipse.tcf.services.ILineNumbers.CodeArea;
import org.eclipse.tcf.services.IMemory;
import org.eclipse.tcf.services.IRegisters;
import org.eclipse.tcf.services.IRegisters.RegistersContext;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.services.IRunControl.RunControlContext;
import org.eclipse.tcf.services.IRunControl.RunControlListener;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;

/**
 * A {@link ISuspendResume} adapter for TCF execution contexts enabling special
 * run control actions run-to-line, move-to-line and resume-at-line.
 */
@SuppressWarnings("restriction")
public class TCFSuspendResumeAdapter implements ISuspendResume, IRunToLine,
        IRunToAddress, IMoveToLine, IMoveToAddress, IResumeAtLine,
        IResumeAtAddress, IAdaptable {

    private static class Location {
        String file;
        int line;
        Number address;
        Location(String file, int line) {
            this.file = file;
            this.line = line;
        }
        Location(Number address) {
            this.address = address;
        }
    }

    private final TCFNodeExecContext exec_ctx;

    public TCFSuspendResumeAdapter(TCFNodeExecContext exec_ctx) {
        this.exec_ctx = exec_ctx;
    }

    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class adapter) {
        if (adapter.isInstance(this)) {
            return this;
        }
        return null;
    }

    public boolean canResume() {
        return isSuspended();
    }

    public boolean canSuspend() {
        return !isSuspended();
    }

    public boolean isSuspended() {
        try {
            Boolean result = new TCFTask<Boolean>() {
                public void run() {
                    if (exec_ctx.isDisposed()) {
                        done(Boolean.FALSE);
                        return;
                    }
                    TCFDataCache<TCFContextState> state = exec_ctx.getState();
                    if (!state.validate(this)) {
                        return;
                    }
                    if (state.getError() == null && state.getData() != null) {
                        done(state.getData().is_suspended);
                        return;
                    }
                    done(Boolean.FALSE);
                }
            }.get();
            return result != null ? result : false;
        }
        catch (Exception e) {
            // ignored
        }
        return false;
    }

    public void resume() throws DebugException {
        new TCFDebugTask<Object>() {
            public void run() {
                if (exec_ctx.isDisposed()) {
                    // ignore silently
                    done(null);
                    return;
                }
                TCFDataCache<IRunControl.RunControlContext> cache = exec_ctx.getRunContext();
                if (!cache.validate(this)) {
                    return;
                }
                IRunControl.RunControlContext runCtx = cache.getData();
                runCtx.resume(IRunControl.RM_RESUME, 1, new IRunControl.DoneCommand() {
                    public void doneCommand(IToken token, Exception error) {
                        if (error != null) {
                            error(error);
                        }
                        else {
                            done(null);
                        }
                    }
                });
            }
        }.getD();
    }

    public void suspend() throws DebugException {
        new TCFDebugTask<Object>() {
            public void run() {
                if (exec_ctx.isDisposed()) {
                    // ignore silently
                    done(null);
                    return;
                }
                TCFDataCache<IRunControl.RunControlContext> cache = exec_ctx.getRunContext();
                if (!cache.validate(this)) {
                    return;
                }
                IRunControl.RunControlContext runCtx = cache.getData();
                if (runCtx.canSuspend()) {
                    runCtx.suspend(new IRunControl.DoneCommand() {
                        public void doneCommand(IToken token, Exception error) {
                            if (error != null) {
                                error(error);
                            }
                            else {
                                done(null);
                            }
                        }
                    });
                }
            }
        }.getD();
    }

    public boolean canResumeAtAddress(IAddress address) {
        return true;
    }

    public void resumeAtAddress(IAddress address) throws DebugException {
        moveToLocation(new Location(address.getValue()), true);
    }

    public boolean canResumeAtLine(IFile file, int lineNumber) {
        return true;
    }

    public void resumeAtLine(IFile file, int lineNumber) throws DebugException {
        IPath location = file.getLocation();
        if (location == null) {
            throw new DebugException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Cannot resume at line: Not a local file."));
        }
        resumeAtLine(location.toOSString(), lineNumber);
    }

    public boolean canResumeAtLine(String fileName, int lineNumber) {
        return true;
    }

    public void resumeAtLine(String fileName, int lineNumber) throws DebugException {
        String debuggerPath = mapToDebuggerPath(fileName);
        moveToLocation(new Location(debuggerPath, lineNumber), true);
    }

    public boolean canMoveToAddress(IAddress address) {
        return true;
    }

    public void moveToAddress(IAddress address) throws DebugException {
        moveToLocation(new Location(address.getValue()), false);
    }

    public boolean canMoveToLine(String fileName, int lineNumber) {
        return true;
    }

    public void moveToLine(String fileName, int lineNumber) throws DebugException {
        String debuggerPath = mapToDebuggerPath(fileName);
        moveToLocation(new Location(debuggerPath, lineNumber), false);
    }

    public boolean canRunToAddress(IAddress address) {
        return canResume();
    }

    public void runToAddress(IAddress address, boolean skipBreakpoints) throws DebugException {
        runToLocation(new Location(address.getValue()), skipBreakpoints);
    }

    public boolean canRunToLine(IFile file, int lineNumber) {
        return canResume();
    }

    public void runToLine(IFile file, int lineNumber, boolean skipBreakpoints) throws DebugException {
        IPath location = file.getLocation();
        if (location == null) {
            throw new DebugException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Cannot run to line: Not a local file."));
        }
        runToLine(location.toOSString(), lineNumber, skipBreakpoints);
    }

    public boolean canRunToLine(String fileName, int lineNumber) {
        return canResume();
    }

    public void runToLine(String fileName, final int lineNumber, final boolean skipBreakpoints) throws DebugException {
        String debuggerPath = mapToDebuggerPath(fileName);
        runToLocation(new Location(debuggerPath, lineNumber), skipBreakpoints);
    }

    private String mapToDebuggerPath(String fileName) {
        ISourceLocator locator = exec_ctx.getModel().getLaunch().getSourceLocator();
        if (locator instanceof CSourceLookupDirector) {
            IPath compilationPath = ((CSourceLookupDirector) locator).getCompilationPath(fileName);
            if (compilationPath != null) {
                return compilationPath.toString();
            }
        }
        // fallback: use basename
        return new Path(fileName).lastSegment();
    }

    private void runToLocation(final Location location, final boolean skipBreakpoints) throws DebugException {
        new TCFDebugTask<Object>() {
            public void run() {
                if (exec_ctx.isDisposed()) {
                    // ignore silently
                    done(null);
                    return;
                }
                final IChannel channel = exec_ctx.getChannel();
                final IBreakpoints breakpoints = channel.getRemoteService(IBreakpoints.class);
                if (breakpoints == null) {
                    error("Cannot set breakpoint.");
                    return;
                }
                final String contextId = exec_ctx.getID();
                Map<String, Object> properties = new HashMap<String, Object>();
                if (location.file != null) {
                    properties.put(IBreakpoints.PROP_FILE, location.file);
                    properties.put(IBreakpoints.PROP_LINE, location.line);
                }
                else {
                    properties.put(IBreakpoints.PROP_LOCATION, location.address.toString());
                }
                properties.put(IBreakpoints.PROP_CONTEXT_IDS, new String[] { contextId });
                properties.put(IBreakpoints.PROP_ENABLED, Boolean.TRUE);
                properties.put(IBreakpoints.PROP_SERVICE, IRunControl.NAME);
                final String breakpointId = TCFAction.STEP_BREAKPOINT_PREFIX + contextId;
                properties.put(IBreakpoints.PROP_ID, breakpointId);
                breakpoints.add(properties, new IBreakpoints.DoneCommand() {
                    public void doneCommand(IToken token, Exception error) {
                        if (error != null) {
                            error(error);
                            return;
                        }
                        final Runnable removeBreakpoint = new Runnable() {
                            public void run() {
                                breakpoints.remove(new String[] { breakpointId }, new IBreakpoints.DoneCommand() {
                                    public void doneCommand(IToken token, Exception error) {
                                        // ignore errors?
                                    }
                                });
                            }
                        };
                        Runnable resume = new Runnable() {
                            public void run() {
                                final IRunControl runControl = channel.getRemoteService(IRunControl.class);
                                if (runControl == null) {
                                    error("Cannot resume.");
                                    removeBreakpoint.run();
                                    return;
                                }
                                final TCFDataCache<IRunControl.RunControlContext> cache = exec_ctx.getRunContext();
                                if (!cache.validate(this)) {
                                    return;
                                }
                                if (cache.getError() != null) {
                                    error(cache.getError());
                                    removeBreakpoint.run();
                                    return;
                                }
                                runControl.addListener(new RunControlListener() {
                                    private void finished() {
                                        runControl.removeListener(this);
                                        removeBreakpoint.run();
                                    }
                                    public void contextSuspended(String context, String pc, String reason,
                                            Map<String, Object> params) {
                                        if (contextId.equals(context)) {
                                            finished();
                                        }
                                    }
                                    public void contextResumed(String context) {
                                    }
                                    public void contextRemoved(String[] context_ids) {
                                        for (String context : context_ids) {
                                            if (contextId.equals(context)) {
                                                finished();
                                                return;
                                            }
                                        }
                                    }
                                    public void contextException(String context, String msg) {
                                    }
                                    public void contextChanged(RunControlContext[] contexts) {
                                    }
                                    public void contextAdded(RunControlContext[] contexts) {
                                    }
                                    public void containerSuspended(String context, String pc, String reason,
                                            Map<String, Object> params, String[] suspended_ids) {
                                        for (String context2 : suspended_ids) {
                                            if (contextId.equals(context2)) {
                                                finished();
                                                return;
                                            }
                                        }
                                    }
                                    public void containerResumed(String[] context_ids) {
                                    }
                                });
                                IRunControl.RunControlContext runCtx = cache.getData();
                                runCtx.resume(IRunControl.RM_RESUME, 1, new IRunControl.DoneCommand() {
                                    public void doneCommand(IToken token, Exception error) {
                                        if (error != null) {
                                            error(error);
                                        }
                                        else {
                                            done(null);
                                        }
                                    }
                                });
                            }
                        };
                        resume.run();
                    }
                });
            }
        }.getD();
    }

    private void moveToLocation(final Location location, final boolean resume) throws DebugException {
        new TCFDebugTask<Object>() {
            private RegistersContext pc_reg_ctx;
            public void run() {
                if (exec_ctx.isDisposed()) {
                    // ignore silently
                    done(null);
                    return;
                }

                LinkedList<TCFChildren> queue = new LinkedList<TCFChildren>();
                queue.add(exec_ctx.getRegisters());
                while (pc_reg_ctx == null && !queue.isEmpty()) {
                    TCFChildren regNodesCache = queue.removeFirst();
                    if (!regNodesCache.validate(this)) return;
                    Map<String,TCFNode> regNodes = regNodesCache.getData();
                    if (regNodes == null || regNodes.size() == 0) {
                        if (regNodesCache.getError() != null) error(regNodesCache.getError());
                        else error("Cannot retrive registers info");
                        return;
                    }

                    for (TCFNode node : regNodes.values()) {
                        TCFNodeRegister regNode = (TCFNodeRegister)node;
                        TCFDataCache<IRegisters.RegistersContext> regCtxCache = regNode.getContext();
                        if (!regCtxCache.validate(this)) return;
                        IRegisters.RegistersContext context = regCtxCache.getData();
                        if (context != null && IRegisters.ROLE_PC.equals(context.getRole())) {
                            pc_reg_ctx = context;
                            break;
                        }
                        queue.add(regNode.getChildren());
                    }
                }

                if (pc_reg_ctx == null) {
                    error("Cannot determine PC register.");
                    return;
                }

                if (location.address == null) {
                    final IChannel channel = exec_ctx.getChannel();
                    final ILineNumbers lineNumbers = channel.getRemoteService(ILineNumbers.class);
                    if (lineNumbers == null) {
                        error("No line numbers service.");
                        return;
                    }
                    TCFDataCache<TCFNodeExecContext> memNodeCache = exec_ctx.getMemoryNode();
                    if (!memNodeCache.validate(this)) return;
                    TCFNodeExecContext memNode = memNodeCache.getData();
                    if (memNode == null) {
                        if (memNodeCache.getError() != null) error(memNodeCache.getError());
                        else error("Cannot determine memory context.");
                        return;
                    }
                    TCFDataCache<IMemory.MemoryContext> memCtxCache = memNode.getMemoryContext();
                    if (!memCtxCache.validate(this)) return;
                    final IMemory.MemoryContext memCtx = memCtxCache.getData();
                    if (memCtx == null) {
                        if (memNodeCache.getError() != null) error(memNodeCache.getError());
                        else error("Cannot determine memory context.");
                        return;
                    }
                    lineNumbers.mapToMemory(memCtx.getID(), location.file, location.line, 0, new ILineNumbers.DoneMapToMemory() {
                        public void doneMapToMemory(IToken token, Exception error, CodeArea[] areas) {
                            if (error != null) {
                                error(error);
                                return;
                            }
                            CodeArea area = null;
                            if (areas != null) {
                                for (CodeArea a : areas) {
                                    if (area == null) {
                                        area = a;
                                        continue;
                                    }
                                    if (!area.is_statement && a.is_statement) {
                                        area = a;
                                        continue;
                                    }
                                    if (a.is_statement && a.start_line == location.line &&
                                            (a.start_line != area.start_line || a.start_column < area.start_column)) {
                                        area = a;
                                        continue;
                                    }
                                }
                            }
                            if (area == null) {
                                error("Cannot map source location to address.");
                                return;
                            }
                            if (area.start_address.equals(area.end_address) || area.start_line != location.line) {
                                if (area.next_stmt_address != null) doneGetLocationAddress(area.next_stmt_address);
                                else doneGetLocationAddress(area.end_address);
                            }
                            else {
                                doneGetLocationAddress(area.start_address);
                            }
                        }
                    });
                }
                else {
                    doneGetLocationAddress(location.address);
                }
            }
            private void doneGetLocationAddress(Number address) {
                if (address == null) {
                    error("Cannot map source location to address.");
                    return;
                }
                byte[] value = addressToByteArray(address, pc_reg_ctx.getSize(), pc_reg_ctx.isBigEndian());
                pc_reg_ctx.set(value, new IRegisters.DoneSet() {
                    public void doneSet(IToken token, Exception error) {
                        if (error != null) {
                            error(error);
                            return;
                        }
                        exec_ctx.getModel().setDebugViewSelection(exec_ctx, "Move");
                        if (resume) {
                            final TCFDataCache<IRunControl.RunControlContext> cache = exec_ctx.getRunContext();
                            final IChannel channel = exec_ctx.getChannel();
                            Runnable resume = new Runnable() {
                                public void run() {
                                    final IRunControl runControl = channel.getRemoteService(IRunControl.class);
                                    if (runControl == null) {
                                        error("Cannot resume.");
                                        return;
                                    }
                                    if (!cache.validate(this)) {
                                        return;
                                    }
                                    if (cache.getError() != null) {
                                        error(cache.getError());
                                        return;
                                    }
                                    IRunControl.RunControlContext runCtx = cache.getData();
                                    runCtx.resume(IRunControl.RM_RESUME, 1, new IRunControl.DoneCommand() {
                                        public void doneCommand(IToken token, Exception error) {
                                            if (error != null) {
                                                error(error);
                                            }
                                            else {
                                                done(null);
                                            }
                                        }
                                    });
                                }
                            };
                            if (cache.validate(resume)) {
                                resume.run();
                            }
                        }
                        else {
                            done(null);
                        }
                    }
                });
            }
            private byte[] addressToByteArray(Number address, int size, boolean bigEndian) {
                byte[] bytes = new byte[size];
                byte[] addrBytes = JSON.toBigInteger(address).toByteArray();
                for (int i=0; i < bytes.length; ++i) {
                    byte b = 0;
                    if (i < addrBytes.length) {
                        b = addrBytes[addrBytes.length - i - 1];
                    }
                    bytes[bigEndian ? size -i - 1 : i] = b;
                }
                return bytes;
            }
        }.getD();
    }
}
