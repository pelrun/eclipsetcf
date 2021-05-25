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
package org.eclipse.tcf.internal.debug.ui.commands;

import java.util.Map;

import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.debug.core.commands.IDropToFrameHandler;
import org.eclipse.debug.core.commands.IEnabledStateRequest;
import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeStackFrame;
import org.eclipse.tcf.internal.debug.ui.model.TCFRunnable;
import org.eclipse.tcf.services.IBreakpoints;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.services.IRunControl.RunControlContext;
import org.eclipse.tcf.util.TCFDataCache;

/**
 * Drop-to-frame command handler for TCF.
 */
public class DropToFrameCommand implements IDropToFrameHandler {

    private final TCFModel model;

    public DropToFrameCommand(TCFModel model) {
        this.model = model;
    }

    public void canExecute(final IEnabledStateRequest request) {
        new TCFRunnable(model, request) {
            public void run() {
                Object[] elements = request.getElements();
                if (elements.length != 1 || !(elements[0] instanceof TCFNodeStackFrame)) {
                    request.setEnabled(false);
                    done();
                    return;
                }
                TCFNodeStackFrame frame_node = (TCFNodeStackFrame)elements[0];
                TCFNodeExecContext exe_node = (TCFNodeExecContext)frame_node.getParent();
                if (!exe_node.getStackTrace().validate(this)) return;
                if (frame_node.getFrameNo() < 1) {
                    request.setEnabled(false);
                    done();
                    return;
                }
                TCFDataCache<IRunControl.RunControlContext> ctx_cache = exe_node.getRunContext();
                if (!ctx_cache.validate(this)) {
                    return;
                }
                IRunControl.RunControlContext ctx = ctx_cache.getData();
                if (!canStepOut(ctx)) {
                    request.setEnabled(false);
                    done();
                    return;
                }
                int action_cnt = model.getLaunch().getContextActionsCount(ctx.getID());
                if (action_cnt > 0 || !canStepOut(ctx)) {
                    request.setEnabled(false);
                    done();
                    return;
                }
                TCFDataCache<TCFContextState> state_cache = exe_node.getMinState();
                if (!state_cache.validate(this)) return;
                TCFContextState state_data = state_cache.getData();
                request.setEnabled(state_data != null && state_data.is_suspended);
                done();
            }

            private boolean canStepOut(RunControlContext ctx) {
                if (ctx == null) return false;
                if (ctx.canResume(IRunControl.RM_STEP_OUT)) return true;
                if (!ctx.hasState()) return false;
                if (ctx.canResume(IRunControl.RM_RESUME) && model.getLaunch().getService(IBreakpoints.class) != null) return true;
                return false;
            }
        };
    }

    public boolean execute(final IDebugCommandRequest request) {
        new TCFRunnable(model, request) {
            public void run() {
                Object[] elements = request.getElements();
                if (elements.length != 1 || !(elements[0] instanceof TCFNodeStackFrame)) {
                    request.setStatus(Status.CANCEL_STATUS);
                    done();
                    return;
                }
                final TCFNodeStackFrame frame_node = (TCFNodeStackFrame)elements[0];
                TCFNodeExecContext exe_node = (TCFNodeExecContext)frame_node.getParent();
                if (!exe_node.getStackTrace().validate(this)) return;
                int frameNo = frame_node.getFrameNo();
                if (frameNo < 1) {
                    request.setStatus(Status.CANCEL_STATUS);
                    done();
                    return;
                }
                TCFDataCache<IRunControl.RunControlContext> ctx_cache = exe_node.getRunContext();
                if (!ctx_cache.validate(this)) return;
                TCFDataCache<TCFContextState> state_cache = exe_node.getMinState();
                if (!state_cache.validate(this)) return;
                TCFContextState state_data = state_cache.getData();
                if (state_data == null || !state_data.is_suspended) {
                    request.setStatus(Status.CANCEL_STATUS);
                    done();
                    return;
                }
                Map<String, TCFNode> stack = exe_node.getStackTrace().getData();
                for (TCFNode node : stack.values()) {
                    TCFNodeStackFrame frame_to_step_out = (TCFNodeStackFrame) node;
                    if (frame_to_step_out.getFrameNo() == frameNo - 1) {
                        new ActionStepOut(exe_node, false, frame_to_step_out, request, new Runnable() {
                            public void run() {
                                request.done();
                            }
                        });
                        return;
                    }
                }
                request.setStatus(Status.CANCEL_STATUS);
                done();
            }
        };
        return false;
    }
}
