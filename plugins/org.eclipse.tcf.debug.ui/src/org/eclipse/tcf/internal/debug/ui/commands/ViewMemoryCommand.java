/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.commands;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IMemoryBlockManager;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockRetrievalExtension;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRenderingContainer;
import org.eclipse.debug.ui.memory.IMemoryRenderingManager;
import org.eclipse.debug.ui.memory.IMemoryRenderingSite;
import org.eclipse.debug.ui.memory.IMemoryRenderingType;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tcf.internal.debug.ui.Activator;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.internal.debug.ui.model.TCFModelProxy;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExpression;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeRegister;
import org.eclipse.tcf.internal.debug.ui.model.TCFNumberFormat;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.JSON;
import org.eclipse.tcf.services.IExpressions;
import org.eclipse.tcf.services.IRegisters;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;

public class ViewMemoryCommand extends AbstractActionDelegate {

    private static class Block {
        TCFNode node;
        BigInteger addr;
        long size;
    }

    private final Runnable on_value_changed = new Runnable() {
        @Override
        public void run() {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    selectionChanged();
                }
            });
        }
    };

    private Block getBlockInfo(final TCFNode node) {
        try {
            return new TCFTask<Block>(node.getChannel()) {
                public void run() {
                    try {
                        TCFModel model = node.getModel();
                        TCFNode mem_node = node;
                        BigInteger addr = null;
                        long size = -1;
                        if (node instanceof TCFNodeExpression && ((TCFNodeExpression)node).isEnabled()) {
                            TCFDataCache<IExpressions.Value> val_cache = ((TCFNodeExpression)node).getValue();
                            if (!val_cache.validate(on_value_changed)) {
                                /* Don't wait until the value is evaluated -
                                 * it can take long time if the expression contains a function call. */
                                done(null);
                                return;
                            }
                            IExpressions.Value val_data = val_cache.getData();
                            if (val_data != null) {
                                addr = JSON.toBigInteger(val_data.getAddress());
                                if (addr != null) {
                                    byte[] bytes = val_data.getValue();
                                    if (bytes != null) size = bytes.length;
                                }
                                if (addr == null) {
                                    String id = val_data.getRegisterID();
                                    if (val_data.getRegisterID() != null) {
                                        if (!model.createNode(id, this)) return;
                                        TCFNodeRegister reg_node = (TCFNodeRegister)model.getNode(id);
                                        if (reg_node != null) {
                                            TCFDataCache<IRegisters.RegistersContext> reg_cache = reg_node.getContext();
                                            if (!reg_cache.validate(this)) return;
                                            IRegisters.RegistersContext reg_data = reg_cache.getData();
                                            if (reg_data != null) {
                                                String mem_id = reg_data.getMemoryContext();
                                                if (mem_id != null) {
                                                    if (!model.createNode(mem_id, this)) return;
                                                    addr = JSON.toBigInteger(reg_data.getMemoryAddress());
                                                    mem_node = model.getNode(mem_id);
                                                }
                                            }
                                        }
                                    }
                                }
                                if (addr == null) {
                                    @SuppressWarnings("unchecked")
                                    List<Map<String,Object>> pieces = (List<Map<String,Object>>)val_data.getProperties().get(IExpressions.VAL_PIECES);
                                    if (pieces != null) {
                                        for (Map<String,Object> props : pieces) {
                                            addr = JSON.toBigInteger((Number)props.get("Address"));
                                            if (addr != null) {
                                                Number n = (Number)props.get("Size");
                                                if (n != null) size = n.longValue();
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (addr == null) {
                                    byte[] bytes = val_data.getValue();
                                    if (bytes != null && bytes.length > 0) {
                                        addr = TCFNumberFormat.toBigInteger(bytes, val_data.isBigEndian(), false);
                                    }
                                }
                            }
                        }
                        else if (node instanceof TCFNodeRegister) {
                            TCFNodeRegister reg_node = (TCFNodeRegister)node;
                            TCFDataCache<IRegisters.RegistersContext> reg_cache = reg_node.getContext();
                            if (!reg_cache.validate(this)) return;
                            IRegisters.RegistersContext reg_data = reg_cache.getData();
                            if (reg_data != null) {
                                String mem_id = reg_data.getMemoryContext();
                                if (mem_id != null) {
                                    if (!model.createNode(mem_id, this)) return;
                                    addr = JSON.toBigInteger(reg_data.getMemoryAddress());
                                    mem_node = model.getNode(mem_id);
                                }
                                if (addr == null) {
                                    TCFDataCache<byte[]> val_cache = reg_node.getValue();
                                    if (!val_cache.validate(this)) return;
                                    byte[] bytes = val_cache.getData();
                                    if (bytes != null && bytes.length > 0) {
                                        addr = TCFNumberFormat.toBigInteger(bytes, reg_data.isBigEndian(), false);
                                    }
                                }
                            }
                        }
                        Block b = null;
                        if (addr != null && mem_node != null) {
                            b = new Block();
                            b.node = mem_node;
                            b.addr = addr;
                            b.size = size;
                        }
                        done(b);
                    }
                    catch (Exception x) {
                        error(x);
                    }
                }
            }.get();
        }
        catch (Exception x) {
            if (node.getChannel().getState() != IChannel.STATE_OPEN) return null;
            Activator.log("Cannot get memory address", x);
            return null;
        }
    }

    private void createRenderingInContainer(IViewPart view, IMemoryBlock mb, IMemoryRenderingType type, String pane) {
        try {
            if (view instanceof IMemoryRenderingSite) {
                IMemoryRendering rendering = type.createRendering();
                IMemoryRenderingContainer container = ((IMemoryRenderingSite)view).getContainer(pane);
                rendering.init(container, mb);
                container.addMemoryRendering(rendering);
            }
        }
        catch (CoreException x) {
            Activator.log(x);
        }
    }

    private void addDefaultRenderings(IViewPart view, IMemoryBlock memoryBlock) {
        IMemoryRenderingManager manager = DebugUITools.getMemoryRenderingManager();
        IMemoryRenderingType primary_type = manager.getPrimaryRenderingType(memoryBlock);
        IMemoryRenderingType default_types[] = manager.getDefaultRenderingTypes(memoryBlock);

        // create primary rendering
        if (primary_type != null) {
            createRenderingInContainer(view, memoryBlock, primary_type, IDebugUIConstants.ID_RENDERING_VIEW_PANE_1);
        }
        else if (default_types.length > 0) {
            primary_type = default_types[0];
            createRenderingInContainer(view, memoryBlock, default_types[0], IDebugUIConstants.ID_RENDERING_VIEW_PANE_1);
        }

        for (IMemoryRenderingType type : default_types) {
            if (!primary_type.getId().equals(type.getId())) {
                createRenderingInContainer(view, memoryBlock, type, IDebugUIConstants.ID_RENDERING_VIEW_PANE_2);
            }
        }
    }

    @Override
    protected void run() {
        try {
            IWorkbenchPage page = getWindow().getActivePage();
            IViewPart view = page.showView(IDebugUIConstants.ID_MEMORY_VIEW, null, IWorkbenchPage.VIEW_ACTIVATE);
            ArrayList<IMemoryBlock> list = new ArrayList<IMemoryBlock>();
            for (TCFNode node : getSelectedNodes()) {
                final Block b = getBlockInfo(node);
                if (b != null) {
                    IMemoryBlockRetrievalExtension mem_retrieval = (IMemoryBlockRetrievalExtension)
                            b.node.getAdapter(IMemoryBlockRetrievalExtension.class);
                    if (mem_retrieval != null) {
                        list.add(mem_retrieval.getMemoryBlock(b.addr.longValue(), b.size));
                        new TCFTask<Boolean>(node.getChannel()) {
                            @Override
                            public void run() {
                                for (TCFModelProxy p : b.node.getModel().getModelProxies()) {
                                    IPresentationContext c = p.getPresentationContext();
                                    if (c.getWindow() != getWindow()) continue;
                                    if (!IDebugUIConstants.ID_DEBUG_VIEW.equals(c.getId())) continue;
                                    p.setSelection(b.node);
                                }
                            }
                        };
                    }
                }
            }
            IMemoryBlockManager manager = DebugPlugin.getDefault().getMemoryBlockManager();
            for (IMemoryBlock mb : list) {
                IMemoryBlock[] mb_array = new IMemoryBlock[] { mb };
                manager.addMemoryBlocks(mb_array);
                addDefaultRenderings(view, mb);
            }
        }
        catch (Exception x) {
            Activator.log("Cannot open memory view", x);
        }
    }

    @Override
    protected void selectionChanged() {
        int cnt = 0;
        for (TCFNode node : getSelectedNodes()) {
            Block b = getBlockInfo(node);
            if (b != null) {
                IMemoryBlockRetrievalExtension mem_retrieval = (IMemoryBlockRetrievalExtension)
                        b.node.getAdapter(IMemoryBlockRetrievalExtension.class);
                if (mem_retrieval != null) cnt++;
            }
        }
        setEnabled(cnt > 0);
    }
}
