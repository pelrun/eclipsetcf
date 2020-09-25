/*******************************************************************************
 * Copyright (c) 2019-2020.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.services.IContextReset;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

public class ResetMenu extends CompoundContributionItem implements IWorkbenchContribution {

    private static final IContributionItem[] EMPTY_MENU = new IContributionItem[0];

    private IServiceLocator serviceLocator;

    @Override
    public void initialize(IServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    static IStructuredSelection getDebugContext() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        ISelection selection = DebugUITools.getDebugContextManager().getContextService(window).getActiveContext();
        if (selection != null && selection instanceof IStructuredSelection) {
            return (IStructuredSelection) selection;
        }
        return StructuredSelection.EMPTY;
    }

    private IContributionItem makeContributionItem(TCFNodeExecContext exec, Map<String, Object> capability) {
        String type = capability.get(IContextReset.CAPABILITY_TYPE).toString();
        String desc = capability.get(IContextReset.CAPABILITY_DESCRIPTION).toString();
        Map<String, Object> params = new HashMap<String, Object>();
        CommandContributionItemParameter itemParameter;

        params.put("org.eclipse.tcf.debug.ui.commands.reset.param.type", type);
        itemParameter = new CommandContributionItemParameter(serviceLocator, null, null, 0);
        itemParameter.commandId = "org.eclipse.tcf.debug.ui.commands.reset";
        itemParameter.parameters = params;
        itemParameter.label = desc;
        return new CommandContributionItem(itemParameter);
    }

    @Override
    protected IContributionItem[] getContributionItems() {
        IStructuredSelection sselection = getDebugContext();
        Object obj = sselection.getFirstElement();
        IContributionItem[] items = EMPTY_MENU;

        if (obj instanceof TCFNode) {
            TCFNode node = (TCFNode) obj;
            while (node != null) {
                if (node instanceof TCFNodeExecContext) {
                    final TCFNodeExecContext exec = (TCFNodeExecContext) node;
                    Collection<Map<String, Object>> capabilities;
                    capabilities = new TCFTask<Collection<Map<String, Object>>>(exec.getChannel()) {
                        @Override
                        public void run() {
                            TCFDataCache<Collection<Map<String, Object>>> cache = exec.getResetCapabilities();
                            if (!cache.validate(this)) return;
                            done(cache.getData());
                        }
                    }.getE();
                    items = new IContributionItem[capabilities.size()];
                    int i = 0;
                    for (Map<String, Object> c : capabilities) {
                        items[i++] = makeContributionItem(exec, c);
                    }
                    break;
                }
                node = node.getParent();
            }
        }
        return items;
    }
}
