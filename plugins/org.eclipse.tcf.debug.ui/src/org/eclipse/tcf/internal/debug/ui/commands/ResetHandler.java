/*******************************************************************************
 * Copyright (c) 2019.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.services.IContextReset;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;

public class ResetHandler extends AbstractHandler {

    private String getDefaultResetType(final TCFNodeExecContext exec) {
        return new TCFTask<String>(exec.getChannel()) {
            @Override
            public void run() {
                TCFDataCache<Collection<Map<String, Object>>> cache = exec.getResetCapabilities();
                if (!cache.validate(this)) {
                    return;
                }
                Collection<Map<String, Object>> caps = cache.getData();
                if (caps == null || caps.isEmpty()) {
                    done(null);
                }
                else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object>[] items = caps.toArray(new Map[caps.size()]);
                    String type = items[0].get(IContextReset.CAPABILITY_TYPE).toString();
                    done(type);
                }
            }
        }.getE();
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = DebugUITools.getDebugContextForEvent(event);
        if (selection != null && selection instanceof IStructuredSelection) {
            IStructuredSelection ssel = (IStructuredSelection)selection;
            Object obj = ssel.getFirstElement();
            if (obj instanceof TCFNode) {
                TCFNode node = (TCFNode)obj;
                while (node != null) {
                    if (node instanceof TCFNodeExecContext) {
                        TCFNodeExecContext exec = (TCFNodeExecContext)node;
                        String type = event.getParameter("org.eclipse.tcf.debug.ui.commands.reset.param.type");
                        if (type == null) type = getDefaultResetType(exec);
                        exec.reset(type, null);
                        break;
                    }
                    node = node.getParent();
                }
            }
        }
        return null;
    }
}
