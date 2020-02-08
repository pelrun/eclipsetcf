/*******************************************************************************
 * Copyright (c) 2019.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import java.util.Collection;
import java.util.HashMap;
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

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = DebugUITools.getDebugContextForEvent(event);
        if (selection != null && selection instanceof IStructuredSelection) {
            IStructuredSelection ssel = (IStructuredSelection)selection;
            Object obj = ssel.getFirstElement();
            if (obj instanceof TCFNode) {
                final String param_type = event.getParameter("org.eclipse.tcf.debug.ui.commands.reset.param.type");
                TCFNode node = (TCFNode)obj;
                while (node != null) {
                    if (node instanceof TCFNodeExecContext) {
                        final TCFNodeExecContext exec = (TCFNodeExecContext)node;
                        new TCFTask<Boolean>() {
                            @Override
                            public void run() {
                                String type = param_type;
                                if (type == null) {
                                    TCFDataCache<Collection<Map<String, Object>>> cache = exec.getResetCapabilities();
                                    if (!cache.validate(this)) return;
                                    Collection<Map<String, Object>> caps = cache.getData();
                                    if (caps != null && !caps.isEmpty()) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object>[] items = caps.toArray(new Map[caps.size()]);
                                        type = items[0].get(IContextReset.CAPABILITY_TYPE).toString();
                                    }
                                }
                                boolean suspend = exec.getModel().getSuspendAfterReset();
                                Map<String, Object> params = new HashMap<String, Object>();
                                if (suspend) params.put(IContextReset.PARAM_SUSPEND, true);
                                exec.reset(type, params);
                                done(true);
                            }
                        }.getE();
                        break;
                    }
                    node = node.getParent();
                }
            }
        }
        return null;
    }
}
