/*******************************************************************************
 * Copyright (c) 2015 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.commands;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.commands.IDebugCommandHandler;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.debug.core.commands.IEnabledStateRequest;
import org.eclipse.tcf.internal.cdt.ui.Activator;
import org.eclipse.tcf.internal.debug.actions.TCFAction;
import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IBreakpoints;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.services.IRunControl.RunControlContext;
import org.eclipse.tcf.services.IRunControl.RunControlListener;
import org.eclipse.tcf.util.TCFDataCache;

public class TCFStepIntoSelectionHandler implements IDebugCommandHandler {

    @Override
    public void canExecute(final IEnabledStateRequest request) {
        final StepIntoSelectionLocation l = new StepIntoSelectionLocation();
        l.getTextLocation(request);

        if (!l.isValid()) {
            request.setEnabled(false);
            request.done();
            return;
        }

        Protocol.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (l.node.isDisposed()) {
                    done(null, false);
                    return;
                }
                TCFDataCache<TCFContextState> cache = l.node.getState();
                if (!cache.validate(this)) return;
                if (cache.getError() != null) {
                    done(cache.getError(), false);
                    return;
                }
                TCFContextState state = cache.getData();
                done(null, state != null && state.is_suspended);
            }
            private void done(Throwable e, boolean enabled) {
                if (e != null) {
                    request.setStatus(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                            IStatus.ERROR, TCFModel.getErrorMessage(e, true), e));
                }
                request.setEnabled(enabled);
                request.done();
            }
        });
    }

    @Override
    public boolean execute(final IDebugCommandRequest request) {
        final StepIntoSelectionLocation l = new StepIntoSelectionLocation();
        l.getTargetFunction(request);

        if (!l.isValid()) {
            request.done();
            return true;
        }

        Protocol.invokeLater(new Runnable() {
            boolean run_to_line_done;
            boolean req_done;
            String bp_id;
            @Override
            public void run() {
                if (l.node.isDisposed()) {
                    done(null);
                    return;
                }
                IChannel channel = l.node.getChannel();
                if (bp_id == null) {
                    IBreakpoints breakpoints = channel.getRemoteService(IBreakpoints.class);
                    if (breakpoints == null) {
                        done(new Exception("Cannot set breakpoint."));
                        return;
                    }
                    Map<String, Object> properties = new HashMap<String, Object>();
                    properties.put(IBreakpoints.PROP_FILE, l.text_file);
                    properties.put(IBreakpoints.PROP_LINE, l.text_line);
                    if (run_to_line_done) {
                        assert l.target_function != null;
                        properties.put(IBreakpoints.PROP_LOCATION, l.target_function.getElementName());
                        properties.put(IBreakpoints.PROP_SKIP_PROLOGUE, true);
                    }
                    properties.put(IBreakpoints.PROP_CONTEXT_IDS, new String[] { l.node.getID() });
                    properties.put(IBreakpoints.PROP_ENABLED, Boolean.TRUE);
                    bp_id = TCFAction.STEP_BREAKPOINT_PREFIX + l.node.getID();
                    properties.put(IBreakpoints.PROP_ID, bp_id);
                    breakpoints.add(properties, new IBreakpoints.DoneCommand() {
                        public void doneCommand(IToken token, Exception error) {
                            if (error != null) {
                                bp_id = null;
                                done(error);
                            }
                            else {
                                run();
                            }
                        }
                    });
                    return;
                }
                final IRunControl run_ctrl = channel.getRemoteService(IRunControl.class);
                if (run_ctrl == null) {
                    done(new Exception("Cannot resume."));
                    return;
                }
                TCFDataCache<TCFContextState> state_cache = l.node.getState();
                if (!state_cache.validate(this)) return;
                if (state_cache.getError() != null) {
                    done(state_cache.getError());
                    return;
                }
                TCFContextState state = state_cache.getData();
                if (state == null || !state.is_suspended) {
                    done(null);
                    return;
                }
                TCFDataCache<IRunControl.RunControlContext> ctx_cache = l.node.getRunContext();
                if (!ctx_cache.validate(this)) return;
                if (ctx_cache.getError() != null) {
                    done(ctx_cache.getError());
                    return;
                }
                final String node_id = l.node.getID();
                run_ctrl.addListener(new RunControlListener() {
                    private void suspended() {
                        run_ctrl.removeListener(this);
                        if (!run_to_line_done) {
                            run_to_line_done = true;
                            if (l.target_function != null) {
                                assert bp_id != null;
                                IChannel channel = l.node.getChannel();
                                IBreakpoints breakpoints = channel.getRemoteService(IBreakpoints.class);
                                breakpoints.remove(new String[] { bp_id }, new IBreakpoints.DoneCommand() {
                                    public void doneCommand(IToken token, Exception error) {
                                        if (error != null) done(error);
                                        else run();
                                    }
                                });
                                bp_id = null;
                                return;
                            }
                        }
                        done(null);
                    }
                    public void contextSuspended(String context, String pc, String reason, Map<String,Object> params) {
                        if (node_id.equals(context)) {
                            suspended();
                        }
                    }
                    public void contextResumed(String context) {
                    }
                    public void contextRemoved(String[] context_ids) {
                        for (String context : context_ids) {
                            if (node_id.equals(context)) {
                                suspended();
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
                    public void containerSuspended(String context, String pc, String reason, Map<String,Object> params, String[] suspended_ids) {
                        for (String context2 : suspended_ids) {
                            if (node_id.equals(context2)) {
                                suspended();
                                return;
                            }
                        }
                    }
                    public void containerResumed(String[] context_ids) {
                    }
                });
                IRunControl.RunControlContext ctx = ctx_cache.getData();
                ctx.resume(IRunControl.RM_RESUME, 1, new IRunControl.DoneCommand() {
                    public void doneCommand(IToken token, Exception error) {
                        if (error != null) {
                            done(error);
                        }
                        else if (!req_done) {
                            req_done = true;
                            request.done();
                        }
                    }
                });
            }
            private void done(Throwable e) {
                if (bp_id != null) {
                    IChannel channel = l.node.getChannel();
                    IBreakpoints breakpoints = channel.getRemoteService(IBreakpoints.class);
                    breakpoints.remove(new String[] { bp_id }, new IBreakpoints.DoneCommand() {
                        public void doneCommand(IToken token, Exception error) {
                            done(error);
                        }
                    });
                    bp_id = null;
                    return;
                }
                if (!req_done) {
                    if (e != null) {
                        request.setStatus(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
                                IStatus.ERROR, TCFModel.getErrorMessage(e, true), e));
                    }
                    req_done = true;
                    request.done();
                }
            }
        });
        return true;
    }
}
