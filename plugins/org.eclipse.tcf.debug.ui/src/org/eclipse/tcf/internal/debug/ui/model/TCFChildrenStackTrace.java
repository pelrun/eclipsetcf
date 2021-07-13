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
package org.eclipse.tcf.internal.debug.ui.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.protocol.IErrorReport;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IStackTrace;
import org.eclipse.tcf.util.TCFDataCache;


public class TCFChildrenStackTrace extends TCFChildren {

    private final TCFNodeExecContext node;
    private final IStackTrace service;

    private String top_frame_id;
    private int limit_factor = 1;

    TCFChildrenStackTrace(TCFNodeExecContext node) {
        super(node, 16);
        this.node = node;
        service = node.model.getLaunch().getService(IStackTrace.class);
    }

    void onSourceMappingChange() {
        for (TCFNode n : getNodes()) ((TCFNodeStackFrame)n).onSourceMappingChange();
    }

    void onExpressionAddedOrRemoved() {
        for (TCFNode n : getNodes()) ((TCFNodeStackFrame)n).onExpressionAddedOrRemoved();
    }

    void onSuspended(boolean func_call) {
        limit_factor = 1;
        for (TCFNode n : getNodes()) ((TCFNodeStackFrame)n).onSuspended(func_call);
        reset();
    }

    void onRegistersChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeStackFrame)n).onRegistersChanged();
        reset();
    }

    void onMemoryMapChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeStackFrame)n).onMemoryMapChanged();
        reset();
    }

    void onMemoryChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeStackFrame)n).onMemoryChanged();
        reset();
    }

    void onRegisterValueChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeStackFrame)n).onRegisterValueChanged();
        reset();
    }

    void onPreferencesChanged() {
        reset();
    }

    int getTraceLimit() {
        int limit_value = 0x7fffffff;
        boolean limit_enabled = node.model.getStackFramesLimitEnabled();
        if (limit_enabled) {
            assert limit_factor > 0;
            limit_value = node.model.getStackFramesLimitValue() * limit_factor;
            if (limit_value <= 0) limit_value = limit_factor;
        }
        return limit_value;
    }

    void riseTraceLimit() {
        limit_factor++;
        // Command arguments changed - cancel pending command
        cancel();
    }

    void postAllChangedDelta() {
        for (TCFNode n : getNodes()) ((TCFNodeStackFrame)n).postAllChangedDelta();
    }

    Boolean checkHasChildren(Runnable done) {
        TCFDataCache<TCFContextState> state = node.getMinState();
        if (!state.validate(done)) return null;
        if (state.getError() != null) return false;
        TCFContextState state_data = state.getData();
        if (state_data == null) return false;
        if (!state_data.is_suspended) return false;
        if (state_data.isNotActive()) return false;
        return true;
    }

    public TCFNodeStackFrame getTopFrame() {
        assert isValid();
        return (TCFNodeStackFrame)node.model.getNode(top_frame_id);
    }

    @Override
    public void set(IToken token, Throwable error, Map<String,TCFNode> data) {
        for (TCFNode n : getNodes()) {
            if (data == null || data.get(n.id) == null) ((TCFNodeStackFrame)n).setFrameNo(-1);
        }
        super.set(token, error, data);
    }

    private void addEmulatedTopFrame(HashMap<String,TCFNode> data) {
        top_frame_id = node.id + "-TF";
        TCFNodeStackFrame n = (TCFNodeStackFrame)node.model.getNode(top_frame_id);
        if (n == null) n = new TCFNodeStackFrame(node, top_frame_id, true);
        n.setFrameNo(0);
        data.put(n.id, n);
    }

    private void runCompleteStackTrace(final HashMap<String,TCFNode> data) {
        command = service.getChildren(node.id, new IStackTrace.DoneGetChildren() {
            public void doneGetChildren(IToken token, Exception error, String[] contexts) {
                if (command == token) {
                    if (error == null && contexts != null) {
                        int limit_value = getTraceLimit();
                        int cnt = contexts.length;
                        for (String id : contexts) {
                            cnt--;
                            if (cnt <= limit_value) {
                                TCFNodeStackFrame n = (TCFNodeStackFrame)node.model.getNode(id);
                                if (n == null) n = new TCFNodeStackFrame(node, id, false);
                                assert n.parent == node;
                                n.setFrameNo(cnt);
                                data.put(id, n);
                                if (cnt == 0) top_frame_id = id;
                            }
                        }
                    }
                    if (data.size() == 0) addEmulatedTopFrame(data);
                    set(token, error, data);
                }
            }
        });
    }

    private void runIncrementalStackTrace(final HashMap<String,TCFNode> data) {
        final int limit_value = getTraceLimit();
        command = service.getChildrenRange(node.id, 0, limit_value, new IStackTrace.DoneGetChildren() {
            public void doneGetChildren(IToken token, Exception error, String[] contexts) {
                if (command == token) {
                    if (error instanceof IErrorReport && ((IErrorReport)error).getErrorCode() == IErrorReport.TCF_ERROR_INV_COMMAND) {
                        node.model.no_incremental_trace = true;
                        runCompleteStackTrace(data);
                        return;
                    }
                    if (error == null && contexts != null) {
                        int cnt = 0;
                        for (String id : contexts) {
                            TCFNodeStackFrame n = (TCFNodeStackFrame)node.model.getNode(id);
                            if (n == null) n = new TCFNodeStackFrame(node, id, false);
                            assert n.parent == node;
                            n.setFrameNo(cnt);
                            data.put(id, n);
                            if (cnt == 0) top_frame_id = id;
                            cnt++;
                        }
                    }
                    if (data.size() == 0) addEmulatedTopFrame(data);
                    set(token, error, data);
                }
            }
        });
    }

    @Override
    protected boolean startDataRetrieval() {
        Boolean has_children = checkHasChildren(this);
        if (has_children == null) return false;
        final HashMap<String,TCFNode> data = new HashMap<String,TCFNode>();
        if (!has_children) {
            top_frame_id = null;
            TCFDataCache<TCFContextState> state = node.getMinState();
            if (!state.validate(this)) return false;
            set(null, state.getError(), data);
            return true;
        }
        if (service == null) {
            addEmulatedTopFrame(data);
            set(null, null, data);
            return true;
        }
        assert command == null;
        if (node.model.no_incremental_trace || !node.model.getStackFramesLimitEnabled()) {
            runCompleteStackTrace(data);
        }
        else {
            runIncrementalStackTrace(data);
        }
        return false;
    }
}
