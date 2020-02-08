/*******************************************************************************
 * Copyright (c) 2013 Xilinx, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Xilinx - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.examples.filtering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.debug.core.IRequest;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenCountUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.tcf.debug.ui.ITCFModel;
import org.eclipse.tcf.debug.ui.ITCFPresentationProvider;
import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.internal.debug.ui.model.TCFChildren;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.internal.debug.ui.model.TCFModelProxy;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.services.IRunControl.RunControlContext;
import org.eclipse.tcf.util.TCFDataCache;

@SuppressWarnings("restriction")
public class PresentationFilter implements ITCFPresentationProvider {

    private static final TCFNode[] pass_through = new TCFNode[0];

    private HashMap<ITCFModel, RCListener> listeners = new HashMap<ITCFModel, RCListener>();

    private class RCListener implements IRunControl.RunControlListener {

        final TCFModel model;

        RCListener(TCFModel model) {
            this.model = model;
            listeners.put(model, this);
        }

        @Override
        public void contextSuspended(String context, String pc, String reason, Map<String, Object> params) {
            postDelta(context);
        }

        @Override
        public void contextResumed(String context) {
            postDelta(context);
        }

        @Override
        public void contextRemoved(String[] context_ids) {
        }

        @Override
        public void contextException(String context, String msg) {
        }

        @Override
        public void contextChanged(RunControlContext[] contexts) {
        }

        @Override
        public void contextAdded(RunControlContext[] contexts) {
        }

        @Override
        public void containerSuspended(String context, String pc, String reason, Map<String, Object> params, String[] suspended_ids) {
            for (String id : suspended_ids)
                postDelta(id);
        }

        @Override
        public void containerResumed(String[] context_ids) {
            for (String id : context_ids)
                postDelta(id);
        }

        void postDelta(String id) {
            TCFNode node = model.getNode(id);
            if (node == null) return;
            node = node.getParent();
            if (node == null) return;
            for (TCFModelProxy p : model.getModelProxies()) {
                String view_id = p.getPresentationContext().getId();
                if (IDebugUIConstants.ID_DEBUG_VIEW.equals(view_id)) {
                    p.addDelta(node, IModelDelta.CONTENT);
                }
            }
        }
    };

    @Override
    public boolean onModelCreated(final ITCFModel m) {
        return true;
    }

    @Override
    public void onModelDisposed(ITCFModel model) {
        RCListener listener = listeners.remove(model);
        if (listener != null) model.getChannel().getRemoteService(IRunControl.class).removeListener(listener);
    }

    @Override
    public boolean updateStarted(IRequest request) {
        return true;
    }

    @Override
    public boolean updateComplete(IRequest request) {
        if (request instanceof IViewerUpdate) {
            IViewerUpdate viewer_update = (IViewerUpdate) request;
            Object element = viewer_update.getElement();
            IPresentationContext presentastion_context = viewer_update.getPresentationContext();
            if (element instanceof TCFNodeExecContext && IDebugUIConstants.ID_DEBUG_VIEW.equals(presentastion_context.getId())) {
                TCFNodeExecContext exe_context = (TCFNodeExecContext) element;
                if (viewer_update instanceof IChildrenCountUpdate) {
                    IChildrenCountUpdate children_count_update = (IChildrenCountUpdate) viewer_update;
                    TCFNode[] nodes = getFilteredChildren(viewer_update, exe_context);
                    if (nodes == pass_through) return true;
                    if (nodes == null) return false;
                    children_count_update.setChildCount(nodes.length);
                }
                else if (viewer_update instanceof IChildrenUpdate) {
                    IChildrenUpdate children_update = (IChildrenUpdate) viewer_update;
                    TCFNode[] nodes = getFilteredChildren(viewer_update, exe_context);
                    if (nodes == pass_through) return true;
                    if (nodes == null) return false;
                    int ofs = children_update.getOffset();
                    int len = children_update.getLength();
                    for (int n = 0; n < len; n++) {
                        int m = n + ofs;
                        if (m < nodes.length) {
                            children_update.setChild(nodes[m], m);
                        }
                        else {
                            children_update.setChild(null, m);
                        }
                    }
                }
            }
        }
        return true;
    }

    private TCFNode[] getFilteredChildren(final IViewerUpdate request, TCFNodeExecContext exe_context) {
        TCFDataCache<?> pending_cache = null;
        ArrayList<TCFNode> list = new ArrayList<TCFNode>();

        TCFDataCache<IRunControl.RunControlContext> rc_ctx_cache = exe_context.getRunContext();
        if (!rc_ctx_cache.validate()) pending_cache = rc_ctx_cache;

        if (pending_cache == null) {
            IRunControl.RunControlContext rc_ctx = rc_ctx_cache.getData();
            if (rc_ctx == null || rc_ctx.hasState()) return pass_through;

            TCFChildren children_cache = exe_context.getChildren();
            if (!children_cache.validate()) pending_cache = children_cache;

            if (pending_cache == null) {
                for (TCFNode node : children_cache.getData().values()) {
                    if (node instanceof TCFNodeExecContext && node.getModel().getActiveAction(node.getID()) == null) {
                        TCFNodeExecContext child = (TCFNodeExecContext) node;
                        TCFDataCache<IRunControl.RunControlContext> child_ctx_cache = child.getRunContext();
                        if (!child_ctx_cache.validate()) {
                            pending_cache = child_ctx_cache;
                            continue;
                        }
                        IRunControl.RunControlContext child_ctx = child_ctx_cache.getData();
                        if (child_ctx != null && child_ctx.hasState()) {
                            TCFDataCache<TCFContextState> child_state_cache = child.getState();
                            if (!child_state_cache.validate()) {
                                pending_cache = child_state_cache;
                                continue;
                            }
                            TCFContextState child_state = child_state_cache.getData();
                            if (child_state != null && !child_state.is_suspended) continue;
                        }
                    }
                    list.add(node);
                }
            }
        }

        if (pending_cache != null) {
            pending_cache.wait(new Runnable() {
                @Override
                public void run() {
                    if (updateComplete(request)) request.done();
                }
            });
            return null;
        }

        TCFModel model = exe_context.getModel();
        if (listeners.get(model) == null) {
            IRunControl rc = model.getChannel().getRemoteService(IRunControl.class);
            rc.addListener(new RCListener(model));
        }

        TCFNode[] arr = list.toArray(new TCFNode[list.size()]);
        Arrays.sort(arr);
        return arr;
    }
}
