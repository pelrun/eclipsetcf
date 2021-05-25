/*******************************************************************************
 * Copyright (c) 2007, 2014 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.model;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelSelectionPolicy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.internal.debug.model.TCFLaunch;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;

class TCFModelSelectionPolicy implements IModelSelectionPolicy {

    private final TCFModel model;

    TCFModelSelectionPolicy(TCFModel model) {
        this.model = model;
    }

    public boolean contains(ISelection selection, IPresentationContext context) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection)selection;
            Object e = ss.getFirstElement();
            if (e instanceof TCFNode) {
                TCFNode n = (TCFNode)e;
                return n.model == model;
            }
            if (e instanceof TCFLaunch) {
                return model.getLaunch() == e;
            }
        }
        return false;
    }

    public boolean isSticky(ISelection selection, IPresentationContext context) {
        // Selection candidate is from another model,
        // return true if it is OK to give up selection.
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection)selection;
            Object e = ss.getFirstElement();
            if (e instanceof TCFNode) return getSuspendReason((TCFNode)e) != null;
        }
        return false;
    }

    private String getSuspendReason(final TCFNode node) {
        return new TCFTask<String>() {
            public void run() {
                TCFNode n = node;
                while (n != null && !n.isDisposed()) {
                    if (n instanceof TCFNodeExecContext) {
                        TCFDataCache<TCFContextState> cache = ((TCFNodeExecContext)n).getMinState();
                        if (!cache.validate(this)) return;
                        TCFContextState state = cache.getData();
                        if (state != null && state.is_suspended) {
                            if (state.suspend_reason == null) {
                                done(IRunControl.REASON_USER_REQUEST);
                            }
                            else {
                                done(state.suspend_reason);
                            }
                            return;
                        }
                    }
                    n = n.parent;
                }
                done(null);
            }
        }.getE();
    }

    public boolean overrides(ISelection existing, ISelection candidate, IPresentationContext context) {
        // Both existing and candidate selection belong to this model.
        // Return true if it is OK to move selection.
        if (IDebugUIConstants.ID_DEBUG_VIEW.equals(context.getId())) {
            if (existing instanceof IStructuredSelection && candidate instanceof IStructuredSelection) {
                Object el_existing = ((IStructuredSelection)existing).getFirstElement();
                Object el_candidate = ((IStructuredSelection)candidate).getFirstElement();
                if (el_existing == null) return true;
                if (el_existing == el_candidate) return true;
                if (el_existing instanceof TCFNode && el_candidate instanceof TCFNode) {
                    if (el_existing instanceof TCFNodeStackFrame && el_candidate instanceof TCFNodeStackFrame) {
                        TCFNodeStackFrame curr = (TCFNodeStackFrame)el_existing;
                        TCFNodeStackFrame next = (TCFNodeStackFrame)el_candidate;
                        if (curr.parent == next.parent) return true;
                    }
                    if (el_existing instanceof TCFNodeStackFrame && el_candidate instanceof TCFNodeExecContext) {
                        TCFNodeStackFrame curr = (TCFNodeStackFrame)el_existing;
                        TCFNodeExecContext next = (TCFNodeExecContext)el_candidate;
                        if (curr.parent == next) return true;
                    }
                    String s1 = getSuspendReason((TCFNode)el_existing);
                    if (s1 == null) return true;
                    String s2 = getSuspendReason((TCFNode)el_candidate);
                    if (s2 == null) return false;
                    if (s2.equals(IRunControl.REASON_USER_REQUEST)) return false;
                    if (s2.equals(IRunControl.REASON_CONTAINER)) return false;
                    if (s1.equals(IRunControl.REASON_USER_REQUEST)) return true;
                    if (s1.equals(IRunControl.REASON_CONTAINER)) return true;
                    return false;
                }
            }
        }
        return true;
    }

    public ISelection replaceInvalidSelection(ISelection existing, ISelection candidate) {
        if (existing instanceof IStructuredSelection && candidate instanceof IStructuredSelection) {
            Object el_existing = ((IStructuredSelection)existing).getFirstElement();
            Object el_candidate = ((IStructuredSelection)candidate).getFirstElement();
            if (el_candidate == null) {
                if (el_existing == null) return new StructuredSelection(model.getLaunch());
                if (el_existing instanceof TCFNode) {
                    TCFNode node = (TCFNode)el_existing;
                    if (node.parent == null || node.parent instanceof TCFNodeLaunch) {
                        return new StructuredSelection(model.getLaunch());
                    }
                    return new StructuredSelection(node.parent);
                }
            }
        }
        return candidate;
    }
}
