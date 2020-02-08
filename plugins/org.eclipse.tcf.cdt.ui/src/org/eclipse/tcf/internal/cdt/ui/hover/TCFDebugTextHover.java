/*******************************************************************************
 * Copyright (c) 2010, 2016 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui.hover;

import java.util.Map;

import org.eclipse.cdt.debug.ui.editors.AbstractDebugTextHover;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.internal.debug.model.TCFContextState;
import org.eclipse.tcf.internal.debug.ui.model.TCFChildren;
import org.eclipse.tcf.internal.debug.ui.model.TCFChildrenStackTrace;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExpression;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeStackFrame;
import org.eclipse.tcf.internal.debug.ui.model.TCFNumberFormat;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IExpressions;
import org.eclipse.tcf.services.IExpressions.DoneCreate;
import org.eclipse.tcf.services.IExpressions.DoneDispose;
import org.eclipse.tcf.services.IExpressions.DoneEvaluate;
import org.eclipse.tcf.services.IExpressions.Expression;
import org.eclipse.tcf.services.IExpressions.Value;
import org.eclipse.tcf.util.TCFDataCache;
import org.eclipse.tcf.util.TCFTask;

/**
 * TCF implementation of debug expression hover for the C/C++ Editor.
 */
public class TCFDebugTextHover extends AbstractDebugTextHover implements ITextHoverExtension2 {

    private TCFNode node;
    private boolean running;

    @Override
    public IInformationControlCreator getHoverControlCreator() {
        if (useExpressionExplorer()) {
            return createExpressionInformationControlCreator();
        }
        else {
            return new IInformationControlCreator() {
                public IInformationControl createInformationControl(Shell parent) {
                    return new DefaultInformationControl(parent, false);
                }
            };
        }
    }

    private IInformationControlCreator createExpressionInformationControlCreator() {
        return new ExpressionInformationControlCreator();
    }

    protected boolean useExpressionExplorer() {
        return true;
    }

    private boolean getNode(TCFNode selection, Runnable done) {
        node = null;
        running = false;
        TCFNodeStackFrame frame = null;
        TCFNodeExecContext exe = null;
        if (selection instanceof TCFNodeStackFrame) {
            frame = (TCFNodeStackFrame)selection;
            exe = (TCFNodeExecContext)frame.getParent();
        }
        else if (selection instanceof TCFNodeExecContext) {
            exe = (TCFNodeExecContext)selection;
        }
        else {
            return true;
        }
        TCFDataCache<TCFContextState> state_cache = exe.getState();
        if (!state_cache.validate(done)) return false;
        TCFContextState state_data = state_cache.getData();
        if (state_data == null || !state_data.is_suspended) {
            if (exe.getModel().getHoverWhileRunning()) {
                running = true;
                node = exe;
            }
        }
        else {
            if (frame == null) {
                TCFChildrenStackTrace stack = exe.getStackTrace();
                if (!stack.validate(done)) return false;
                frame = stack.getTopFrame();
            }
            if (frame != null && !frame.isEmulated()) node = frame;
        }
        return true;
    }

    @Override
    protected boolean canEvaluate() {
        IAdaptable context = getSelectionAdaptable();
        if (context == null) return false;
        final TCFNode selection = (TCFNode)context.getAdapter(TCFNode.class);
        if (selection == null) return false;
        try {
            return new TCFTask<Boolean>(selection.getChannel()) {
                public void run() {
                    if (!getNode(selection, this)) return;
                    done(node != null);
                }
            }.get();
        }
        catch (Exception x) {
            // Problem in Eclipse 3.7:
            // TextViewerHoverManager calls Thread.interrupt(),
            // but it fails to handle InterruptedException.
            // We have to catch and ignore the exception.
        }
        return false;
    }

    public Object getHoverInfo2(ITextViewer viewer, IRegion region) {
        if (!useExpressionExplorer()) return getHoverInfo(viewer, region);
        IAdaptable context = getSelectionAdaptable();
        if (context == null) return null;
        final TCFNode selection = (TCFNode)context.getAdapter(TCFNode.class);
        if (selection == null) return null;
        final String text = getExpressionText(viewer, region);
        if (text == null || text.length() == 0) return null;
        try {
            return new TCFTask<Object>(selection.getChannel()) {
                public void run() {
                    if (!getNode(selection, this)) return;
                    if (node != null) {
                        TCFChildren cache = node.getModel().getHoverExpressionCache(node, text);
                        if (!cache.validate(this)) return;
                        Map<String,TCFNode> nodes = cache.getData();
                        if (nodes != null) {
                            boolean ok = false;
                            for (TCFNode n : nodes.values()) {
                                TCFNodeExpression expr = (TCFNodeExpression)n;
                                if (running) expr.update(this);
                                TCFDataCache<IExpressions.Value> value = expr.getValue();
                                if (!value.validate(this)) return;
                                if (value.getData() != null) ok = true;
                            }
                            if (ok) {
                                done(node);
                                return;
                            }
                        }
                    }
                    done(null);
                }
            }.get();
        }
        catch (Exception x) {
            // Problem in Eclipse 3.7:
            // TextViewerHoverManager calls Thread.interrupt(),
            // but it fails to handle InterruptedException.
            // We have to catch and ignore the exception.
            return null;
        }
    }

    @Override
    protected String evaluateExpression(final String expression) {
        IAdaptable context = getSelectionAdaptable();
        if (context == null) return null;
        final TCFNode selection = (TCFNode)context.getAdapter(TCFNode.class);
        if (selection == null) return null;
        final IChannel channel = selection.getChannel();
        return new TCFTask<String>(channel) {
            public void run() {
                final IExpressions service = channel.getRemoteService(IExpressions.class);
                if (!getNode(selection, this)) return;
                if (service != null && node != null) {
                    service.create(node.getID(), null, expression, new DoneCreate() {
                        public void doneCreate(IToken token, Exception error, final Expression context) {
                            if (error == null) {
                                service.evaluate(context.getID(), new DoneEvaluate() {
                                    public void doneEvaluate(IToken token, Exception error, Value value) {
                                        done(error == null && value != null ? getValueText(value) : null);
                                        service.dispose(context.getID(), new DoneDispose() {
                                            public void doneDispose(IToken token, Exception error) {
                                                // no-op
                                            }
                                        });
                                    }
                                });
                            }
                            else {
                                done(null);
                            }
                        }
                    });
                }
                else {
                    done(null);
                }
            }
        }.getE();
    }

    private static String getValueText(Value value) {
        byte[] data = value.getValue();
        if (data == null) return "N/A";
        switch(value.getTypeClass()) {
        case integer:
            return TCFNumberFormat.toBigInteger(data, value.isBigEndian(), true).toString();
        case real:
            return TCFNumberFormat.toFPString(data, value.isBigEndian());
        case complex:
            return TCFNumberFormat.toComplexFPString(data, value.isBigEndian());
        default:
            return "0x" + TCFNumberFormat.toBigInteger(data, value.isBigEndian(), false).toString(16);
        }
    }
}
