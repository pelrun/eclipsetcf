/*******************************************************************************
 * Copyright (c) 2010, 2011 Wind River Systems, Inc. and others.
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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.tcf.internal.debug.actions.TCFAction;
import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.internal.debug.model.TCFSourceRef;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.model.TCFChildrenStackTrace;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeStackFrame;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ILineNumbers;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.services.IRunControl.RunControlContext;
import org.eclipse.tcf.util.TCFDataCache;

public class ActionStepInto extends TCFAction implements IRunControl.RunControlListener {

    protected final TCFNodeExecContext node;
    private final IDebugCommandRequest monitor;
    private final Runnable done;
    private boolean step_line;
    private boolean step_back;
    private final IRunControl rc;

    private TCFSourceRef source_ref;
    private BigInteger pc0;
    private BigInteger pc1;
    private int step_cnt;
    private boolean second_step_back;
    private boolean final_step;

    protected boolean exited;

    public ActionStepInto(TCFNodeExecContext node, boolean step_line, boolean back_step,
                IDebugCommandRequest monitor, Runnable done) {
        super(node.getModel().getLaunch(), node.getID());
        this.node = node;
        this.step_line = step_line;
        this.step_back = back_step;
        this.monitor = monitor;
        this.done = done;
        rc = launch.getService(IRunControl.class);
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

    private void setSourceRef(TCFSourceRef ref) {
        ILineNumbers.CodeArea area = ref.area;
        if (area != null) {
            pc0 = JSON.toBigInteger(area.start_address);
            pc1 = JSON.toBigInteger(area.end_address);
        }
        else {
            pc0 = null;
            pc1 = null;
        }
        source_ref = ref;
    }

    private void runAction() {
        if (aborted) {
            exit(null);
            return;
        }
        TCFDataCache<TCFContextState> state_cache = node.getState();
        if (state_cache == null) {
            exit(new Exception("Invalid context ID"));
            return;
        }
        if (!state_cache.validate(this)) return;
        if (state_cache.getData() == null || !state_cache.getData().is_suspended) {
            Throwable error = state_cache.getError();
            if (error == null) error = new Exception("Context is not suspended");
            exit(error);
            return;
        }
        if (step_cnt == 0) {
            rc.addListener(this);
        }
        else {
            String reason = state_cache.getData().suspend_reason;
            if (!IRunControl.REASON_STEP.equals(reason)) {
                exit(null, reason);
                return;
            }
        }
        TCFDataCache<IRunControl.RunControlContext> ctx_cache = node.getRunContext();
        if (!ctx_cache.validate(this)) return;
        IRunControl.RunControlContext ctx_data = ctx_cache.getData();
        if (ctx_data == null) {
            exit(ctx_cache.getError());
            return;
        }
        int mode = 0;
        if (!step_line) mode = step_back ? IRunControl.RM_REVERSE_STEP_INTO : IRunControl.RM_STEP_INTO;
        else mode = step_back ? IRunControl.RM_REVERSE_STEP_INTO_LINE : IRunControl.RM_STEP_INTO_LINE;
        if (ctx_data.canResume(mode)) {
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
        if (!step_line) {
            exit(new Exception("Step into is not supported"));
            return;
        }
        TCFChildrenStackTrace stack_trace = node.getStackTrace();
        if (!stack_trace.validate(this)) return;
        if (stack_trace.getData() == null) {
            exit(stack_trace.getError());
            return;
        }
        TCFNodeStackFrame frame = stack_trace.getTopFrame();
        if (source_ref == null) {
            TCFDataCache<TCFSourceRef> line_info = frame.getLineInfo();
            if (!line_info.validate(this)) return;
            TCFSourceRef ref = line_info.getData();
            if (ref == null) {
                step_line = false;
                Protocol.invokeLater(this);
                return;
            }
            if (ref.error != null) {
                exit(ref.error);
                return;
            }
            setSourceRef(ref);
        }
        BigInteger pc = new BigInteger(state_cache.getData().suspend_pc);
        if (step_cnt > 0) {
            if (pc == null || pc0 == null || pc1 == null) {
                exit(null);
                return;
            }
            if (pc.compareTo(pc0) < 0 || pc.compareTo(pc1) >= 0) {
                TCFDataCache<TCFSourceRef> line_info = frame.getLineInfo();
                if (!line_info.validate(this)) return;
                TCFSourceRef ref = line_info.getData();
                if (ref == null || ref.area == null) {
                    exit(null);
                }
                else if (isSameLine(source_ref.area, ref.area)) {
                    setSourceRef(ref);
                }
                else if (step_back && !second_step_back) {
                    // After step back we stop at last instruction of previous line.
                    // Do second step back into line to skip that line.
                    second_step_back = true;
                    setSourceRef(ref);
                }
                else if (step_back && !final_step) {
                    // After second step back we have stepped one instruction more then needed.
                    // Do final step forward to correct that.
                    final_step = true;
                    step_back = false;
                    setSourceRef(ref);
                }
                else {
                    exit(null);
                    return;
                }
            }
        }
        step_cnt++;
        mode = step_back ? IRunControl.RM_REVERSE_STEP_INTO_RANGE : IRunControl.RM_STEP_INTO_RANGE;
        if (ctx_data.canResume(mode) &&
                pc != null && pc0 != null && pc1 != null &&
                pc.compareTo(pc0) >= 0 && pc.compareTo(pc1) < 0) {
            HashMap<String,Object> args = new HashMap<String,Object>();
            args.put(IRunControl.RP_RANGE_START, pc0);
            args.put(IRunControl.RP_RANGE_END, pc1);
            ctx_data.resume(mode, 1, args, new IRunControl.DoneCommand() {
                public void doneCommand(IToken token, Exception error) {
                    if (error != null) exit(error);
                }
            });
            return;
        }
        mode = step_back ? IRunControl.RM_REVERSE_STEP_INTO : IRunControl.RM_STEP_INTO;
        if (ctx_data.canResume(mode)) {
            ctx_data.resume(mode, 1, new IRunControl.DoneCommand() {
                public void doneCommand(IToken token, Exception error) {
                    if (error != null) exit(error);
                }
            });
            return;
        }
        exit(new Exception("Step into is not supported"));
    }

    private boolean isSameLine(ILineNumbers.CodeArea x, ILineNumbers.CodeArea y) {
        if (x == null || y == null) return false;
        if (x.start_line != y.start_line) return false;
        if (x.directory != y.directory && (x.directory == null || !x.directory.equals(y.directory))) return false;
        if (x.file != y.file && (x.file == null || !x.file.equals(y.file))) return false;
        return true;
    }

    protected void exit(Throwable error) {
        if (exited) return;
        exit(error, "Step Into");
        if (error != null && node.getChannel().getState() == IChannel.STATE_OPEN) {
            monitor.setStatus(new Status(IStatus.ERROR,
                    Activator.PLUGIN_ID, 0, "Cannot step: " + error.getLocalizedMessage(), error));
        }
        done.run();
    }

    protected void exit(Throwable error, String reason) {
        if (exited) return;
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
}
