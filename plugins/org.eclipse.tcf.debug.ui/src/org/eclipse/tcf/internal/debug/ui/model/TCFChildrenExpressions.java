/*******************************************************************************
 * Copyright (c) 2008, 2016 Wind River Systems, Inc. and others.
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

import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IWatchExpression;

public class TCFChildrenExpressions extends TCFChildren {

    TCFChildrenExpressions(TCFNode node) {
        super(node, 128);
    }

    void onSuspended(boolean func_call) {
        for (TCFNode n : getNodes()) ((TCFNodeExpression)n).onSuspended(func_call);
    }

    void onRegisterValueChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeExpression)n).onRegisterValueChanged();
    }

    void onMemoryChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeExpression)n).onMemoryChanged();
    }

    void onMemoryMapChanged() {
        for (TCFNode n : getNodes()) ((TCFNodeExpression)n).onMemoryMapChanged();
    }

    private TCFNodeExpression findScript(IExpression e) {
        for (TCFNode n : getNodes()) {
            TCFNodeExpression node = (TCFNodeExpression)n;
            if (e == node.getPlatformExpression() && e.getExpressionText().equals(node.getScript())) {
                return node;
            }
        }
        return null;
    }

    private TCFNodeExpression findEmpty() {
        for (TCFNode n : getNodes()) {
            TCFNodeExpression e = (TCFNodeExpression)n;
            if (e.isEmpty()) return e;
        }
        return null;
    }

    @Override
    protected boolean startDataRetrieval() {
        int cnt = 0;
        HashMap<String,TCFNode> data = new HashMap<String,TCFNode>();
        for (final IExpression e : node.model.getExpressionManager().getExpressions()) {
            TCFNodeExpression n = findScript(e);
            if (n == null) add(n = new TCFNodeExpression(node, null, e, null, null, null, -1, false));
            n.setSortPosition(cnt++);
            if (e instanceof IWatchExpression) n.setEnabled(((IWatchExpression)e).isEnabled());
            data.put(n.id, n);
        }
        TCFNodeExpression n = findEmpty();
        if (n == null) add(n = new TCFNodeExpression(node, null, null, null, null, null, -1, false));
        n.setSortPosition(cnt++);
        data.put(n.id, n);
        set(null, null, data);
        return true;
    }
}
