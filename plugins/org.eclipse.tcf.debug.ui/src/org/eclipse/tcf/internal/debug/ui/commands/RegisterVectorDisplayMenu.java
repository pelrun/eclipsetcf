/*******************************************************************************
 * Copyright (c) 2012-2020 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ITreeModelViewer;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.tcf.internal.debug.ui.model.TCFChildren;
import org.eclipse.tcf.internal.debug.ui.model.TCFModelProxy;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeRegister;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeStackFrame;
import org.eclipse.tcf.services.IRegisters;
import org.eclipse.tcf.util.TCFTask;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;

public class RegisterVectorDisplayMenu extends CompoundContributionItem implements IWorkbenchContribution  {

    private static final int MAX_REG_REPRESENTATIONS = 100;

    private IServiceLocator service_locator;
    private ArrayList<String> elements;
    private Map<String,Map<String,String>> modes;

    @Override
    public void initialize(IServiceLocator service_locator) {
        this.service_locator = service_locator;
    }

    private Object[] getSelectedRegisters() {
        ISelectionService service = (ISelectionService) service_locator.getService(ISelectionService.class);
        if (service != null) {
            ISelection selection = service.getSelection();
            if (selection instanceof IStructuredSelection) {
                return ((IStructuredSelection)selection).toArray();
            }
        }
        return new Object[0];
    }

    @Override
    protected IContributionItem[] getContributionItems() {
        final IWorkbenchPart part = getPart();
        if (part == null) return null;

        final Object[] selection = getSelectedRegisters();

        final TCFNode node = getRootNode(part);
        if (node == null) return null;
        if (!(node instanceof TCFNodeExecContext) && !(node instanceof TCFNodeStackFrame)) return null;

        try {
            new TCFTask<Boolean>(node.getChannel()) {
                public void run() {
                    modes = new LinkedHashMap<String,Map<String,String>>();
                    for (Object obj : selection) {
                        if (obj instanceof TCFNodeRegister) {
                            TCFNodeRegister reg = (TCFNodeRegister)obj;

                            AtomicBoolean grp = new AtomicBoolean();
                            if (!reg.isRepresentationGroup(grp, this)) return;
                            if (!grp.get()) continue;

                            TCFChildren children = reg.getChildren();
                            if (!children.validate(this)) return;
                            for (TCFNode child_node :  children.toArray()) {
                                TCFNodeRegister child_reg = (TCFNodeRegister)child_node;
                                if (!child_reg.getContext().validate(this)) return;
                                IRegisters.RegistersContext child_ctx = child_reg.getContext().getData();
                                String reg_name = child_ctx.getName();
                                if (reg_name.matches("w[0-9]+")) {
                                    String mode = reg_name.substring(1, reg_name.length()) + "bits";
                                    Map<String,String> map = modes.get(mode);
                                    if (map == null) modes.put(mode, map = new HashMap<String,String>());
                                    map.put(reg.getID(), child_ctx.getID());
                                }
                            }
                        }
                    }

                    // if several registers are selected in the register view, we need to show only the common possibilities
                    for (Iterator<Map<String,String>> i = modes.values().iterator(); i.hasNext();) {
                        Map<String,String> map = i.next();
                        for (Object obj : selection) {
                            if (!(obj instanceof TCFNodeRegister)) continue;
                            if (map.containsKey(((TCFNodeRegister)obj).getID())) continue;
                            i.remove();
                            break;
                        }
                    }

                    elements = new ArrayList<String>(modes.keySet());
                    elements.add(0, elements.size() == 0 ? "No vector display options available" : "none");
                    done(Boolean.TRUE);
                }
            }.get();
        }
        catch (Exception x) {
            return null;
        }

        IContributionItem[] items = new IContributionItem[elements.size()];
        for (int i = 0; i < items.length; i++) {
            final int n = i;
            items[i] = new ContributionItem() {
                @Override
                public void fill(final Menu menu, int index) {
                    final MenuItem item = new MenuItem(menu, elements.size() <= 1 ? SWT.NULL : SWT.RADIO);
                    item.setText(elements.get(n));
                    item.setSelection(getRepresentation() == n);
                    item.addSelectionListener(new SelectionListener() {
                        public void widgetSelected(SelectionEvent e) {
                            if (item.getSelection()) setRepresentation(n);
                        }
                        public void widgetDefaultSelected(SelectionEvent e) {
                        }
                    });
                }
            };
        }

        return items;
    }

    private IWorkbenchPart getPart() {
        IPartService partService = (IPartService)service_locator.getService(IPartService.class);
        if (partService != null) return partService.getActivePart();
        return null;
    }

    private TCFNode getRootNode(IWorkbenchPart part) {
        IWorkbenchPartSite site = part.getSite();
        if (site == null || IDebugUIConstants.ID_DEBUG_VIEW.equals(site.getId())) {
            return null;
        }
        if (part instanceof IDebugView) {
            Object input = ((IDebugView)part).getViewer().getInput();
            if (input instanceof TCFNode) return (TCFNode)input;
        }
        return null;
    }

    private ITreeModelViewer getViewer() {
        IWorkbenchPart part = getPart();

        if (part instanceof IDebugView) {
            return (ITreeModelViewer)((IDebugView)part).getViewer();
        }
        return null;
    }

    private int getRepresentation() {
        IWorkbenchPart part = getPart();
        if (part == null) return 0;
        TCFNodeRegister node = null;
        for (Object obj : getSelectedRegisters()) {
            if (obj instanceof TCFNodeRegister) {
                node = (TCFNodeRegister)obj;
                break;
            }
        }
        if (node == null) return 0;
        final String id = node.getID();
        final IPresentationContext ctx = getViewer().getPresentationContext();
        return new TCFTask<Integer>(node.getChannel()) {
            @SuppressWarnings("unchecked")
            public void run() {
                Map<String,String> representation = (Map<String,String>)ctx.getProperty(
                        TCFNodeRegister.PROPERTY_REG_REPRESENTATION);
                if (representation != null) {
                    String rep_id = representation.get(id);
                    if (rep_id != null) {
                        for (Map.Entry<String,Map<String,String>> e : modes.entrySet()) {
                            if (rep_id.equals(e.getValue().get(id))) {
                                done(elements.indexOf(e.getKey()));
                                return;
                            }
                        }
                    }
                }
                done(0);
            }
        }.getE();
    }

    private void setRepresentation(final int n) {
        final IWorkbenchPart part = getPart();
        if (part == null) return;
        final TCFNode node = getRootNode(part);
        if (node == null) return;

        final Object[] selection = getSelectedRegisters();
        ITreeModelViewer viewer = getViewer();
        final IPresentationContext ctx = viewer.getPresentationContext();
        new TCFTask<Object>(node.getChannel()) {
            @SuppressWarnings({ "unchecked", "serial" })
            public void run() {

                Map<String,String> representation = (Map<String,String>)ctx.getProperty(
                        TCFNodeRegister.PROPERTY_REG_REPRESENTATION);

                if (representation == null) {
                    representation = new LinkedHashMap<String,String>() {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<String,String> eldest) {
                            return size() > MAX_REG_REPRESENTATIONS;
                        }
                    };
                    ctx.setProperty(TCFNodeRegister.PROPERTY_REG_REPRESENTATION, representation);
                }

                Map<String,String> map = modes.get(elements.get(n));
                TCFModelProxy[] proxies = node.getModel().getModelProxies(ctx);
                for (Object obj : selection) {
                    if (obj instanceof TCFNodeRegister) {
                        TCFNodeRegister reg = (TCFNodeRegister)obj;
                        String id = reg.getID();
                        String rep_id = map != null ? map.get(id) : null;
                        if (rep_id == null) representation.remove(id);
                        else representation.put(id, rep_id);
                        for (int i = 0; i < proxies.length; i++) {
                            proxies[i].addDelta(reg, IModelDelta.CONTENT);
                            TCFNode n = node.getModel().getNode(rep_id);
                            if (n != null) proxies[i].expand(n);
                        }
                    }
                }
                done(null);
            }
        };
    }
}
