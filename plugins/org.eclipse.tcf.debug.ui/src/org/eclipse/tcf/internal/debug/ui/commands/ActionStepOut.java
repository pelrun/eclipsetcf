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
package org.eclipse.tcf.internal.debug.ui.commands;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.tcf.internal.debug.actions.TCFAction;
import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.model.TCFChildrenStackTrace;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeStackFrame;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IBreakpoints;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.services.IStackTrace;
import org.eclipse.tcf.services.IRunControl.RunControlContext;
import org.eclipse.tcf.util.TCFDataCache;

public class ActionStepOut extends TCFAction implements IRunControl.RunControlListener {

    protected final TCFNodeExecContext node;
    private final IDebugCommandRequest monitor;
    private final Runnable done;
    private final boolean step_back;
    private final TCFNodeStackFrame drop_to_frame;
    private final IRunControl rc;
    private final IBreakpoints bps;

    private int step_cnt;
    private Map<String,Object> bp;

    protected boolean exited;

    public ActionStepOut(TCFNodeExecContext node, boolean step_back, TCFNodeStackFrame drop_to_frame,
            IDebugCommandRequest monitor, Runnable done) {
        super(node.getModel().getLaunch(), node.getID());
        this.node = node;
        this.step_back = step_back;
        this.drop_to_frame = drop_to_frame;
        this.monitor = monitor;
        this.done = done;
        rc = launch.getService(IRunControl.class);
        bps = launch.getService(IBreakpoints.class);
    }

    public void run() {
        if (exited) return;
        try {
            runAction();
        }
        catch (Throwable x) {
            exit(x);
        }
    }

    private void runAction() {
        if (aborted) {
            exit(null);
            return;
        }
        TCFDataCache<TCFContextState> state = node.getState();
        if (state == null) {
            exit(new Exception("Invalid context ID"));
            return;
        }
        if (!state.validate(this)) return;
        if (state.getData() == null || !state.getData().is_suspended) {
            Throwable error = state.getError();
            if (error == null) error = new Exception("Context is not suspended");
            exit(error);
            return;
        }
        if (step_cnt == 0) {
            rc.addListener(this);
        }
        TCFDataCache<IRunControl.RunControlContext> ctx_cache = node.getRunContext();
        if (!ctx_cache.validate(this)) return;
        IRunControl.RunControlContext ctx_data = ctx_cache.getData();
        if (ctx_data == null) {
            exit(ctx_cache.getError());
            return;
        }
        int mode = step_back ? IRunControl.RM_REVERSE_STEP_OUT : IRunControl.RM_STEP_OUT;
        if (drop_to_frame == null && ctx_data.canResume(mode)) {
            if (step_cnt > 0) {
                exit(null);
                return;
            }
            ctx_data.resume(mode, 1, new IRunControl.DoneCommand() {
                public void doneCommand(IToken token, Exception error) {
                    if (error != null) exit(error);
                }
            });
            step_cnt++;
            return;
        }
        TCFChildrenStackTrace stack_trace = node.getStackTrace();
        if (!stack_trace.validate(this)) return;
        if (step_cnt > 0) {
            TCFContextState state_data = state.getData();
            if (isMyBreakpoint(state_data)) {
                exit(null);
                return;
            }
            exit(null, state_data.suspend_reason);
            return;
        }
        if (bps != null && ctx_data.canResume(step_back ? IRunControl.RM_REVERSE_RESUME : IRunControl.RM_RESUME)) {
            if (bp == null) {
                TCFDataCache<IStackTrace.StackTraceContext> frame_cache =
                        (drop_to_frame != null ? drop_to_frame : stack_trace.getTopFrame()).getStackTraceContext();
                if (!frame_cache.validate(this)) return;
                IStackTrace.StackTraceContext frame_data = frame_cache.getData();
                if (frame_data == null) {
                    exit(frame_cache.getError());
                    return;
                }
                Number addr = frame_data.getReturnAddress();
                if (addr == null) {
                    exit(new Exception("Unknown stack frame return address"));
                    return;
                }
                if (step_back) {
                    BigInteger n = JSON.toBigInteger(addr);
                    addr = n.subtract(BigInteger.valueOf(1));
                }
                String id = STEP_BREAKPOINT_PREFIX + ctx_data.getID();
                bp = new HashMap<String,Object>();
                bp.put(IBreakpoints.PROP_ID, id);
                bp.put(IBreakpoints.PROP_LOCATION, addr.toString());
                bp.put(IBreakpoints.PROP_CONDITION, "$thread==\"" + ctx_data.getID() + "\"");
                bp.put(IBreakpoints.PROP_ENABLED, Boolean.TRUE);
                bp.put(IBreakpoints.PROP_SERVICE, IRunControl.NAME);
                bps.add(bp, new IBreakpoints.DoneCommand() {
                    public void doneCommand(IToken token, Exception error) {
                        if (error != null) exit(error);
                    }
                });
            }
            ctx_data.resume(step_back ? IRunControl.RM_REVERSE_RESUME : IRunControl.RM_RESUME, 1, new IRunControl.DoneCommand() {
                public void doneCommand(IToken token, Exception error) {
                    if (error != null) exit(error);
                }
            });
            step_cnt++;
            return;
        }
        exit(new Exception("Step out is not supported"));
    }

    protected void exit(Throwable error) {
        if (exited) return;
        exit(error, "Step Out");
        if (error != null && node.getChannel().getState() == IChannel.STATE_OPEN) {
            monitor.setStatus(new Status(IStatus.ERROR,
                    Activator.PLUGIN_ID, 0, "Cannot step: " + error.getLocalizedMessage(), error));
        }
        done.run();
    }

    protected void exit(Throwable error, String reason) {
        if (exited) return;
        if (bp != null) {
            bps.remove(new String[]{ (String)bp.get(IBreakpoints.PROP_ID) }, new IBreakpoints.DoneCommand() {
                public void doneCommand(IToken token, Exception error) {
                }
            });
        }
        rc.removeListener(this);
        exited = true;
        if (error == null) setActionResult(getContextID(), reason);
        else launch.removeContextActions(getContextID());
        done();
    }

    public void containerResumed(String[] context_ids) {
    }

    public void containerSuspended(String context, String pc,
            String reason, Map<String, Object> params,
            String[] suspended_ids) {
        for (String id : suspended_ids) {
            if (!id.equals(context)) contextSuspended(id, null, null, null);
        }
        contextSuspended(context, pc, reason, params);
    }

    public void contextAdded(RunControlContext[] contexts) {
    }

    public void contextChanged(RunControlContext[] contexts) {
    }

    public void contextException(String context, String msg) {
        if (context.equals(node.getID())) exit(new Exception(msg));
    }

    public void contextRemoved(String[] context_ids) {
        for (String context : context_ids) {
            if (context.equals(node.getID())) exit(null);
        }
    }

    public void contextResumed(String context) {
    }

    public void contextSuspended(String context, String pc, String reason, Map<String,Object> params) {
        if (!context.equals(node.getID())) return;
        Protocol.invokeLater(this);
    }

    private boolean isMyBreakpoint(TCFContextState state_data) {
        if (bp == null) return false;
        if (!IRunControl.REASON_BREAKPOINT.equals(state_data.suspend_reason)) return false;
        if (state_data.suspend_params != null) {
            Object ids = state_data.suspend_params.get(IRunControl.STATE_BREAKPOINT_IDS);
            if (ids != null) {
                @SuppressWarnings("unchecked")
                Collection<String> c = (Collection<String>)ids;
                if (c.contains(bp.get(IBreakpoints.PROP_ID))) return true;
            }
        }
        if (state_data.suspend_pc == null) return false;
        BigInteger x = new BigInteger(state_data.suspend_pc);
        BigInteger y = new BigInteger((String)bp.get(IBreakpoints.PROP_LOCATION));
        return x.equals(y);
    }
}
