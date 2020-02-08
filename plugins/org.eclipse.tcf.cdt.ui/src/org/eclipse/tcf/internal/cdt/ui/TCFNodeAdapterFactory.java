/*******************************************************************************
 * Copyright (c) 2010, 2015 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui;

import java.util.ArrayList;

import org.eclipse.cdt.debug.core.model.IReverseResumeHandler;
import org.eclipse.cdt.debug.core.model.IReverseStepIntoHandler;
import org.eclipse.cdt.debug.core.model.IReverseStepOverHandler;
import org.eclipse.cdt.debug.core.model.IReverseToggleHandler;
import org.eclipse.cdt.debug.core.model.ISteppingModeTarget;
import org.eclipse.cdt.debug.core.model.IUncallHandler;
import org.eclipse.cdt.debug.core.ICWatchpointTarget;
import org.eclipse.cdt.debug.internal.ui.disassembly.dsf.IDisassemblyBackend;
import org.eclipse.cdt.debug.ui.IPinProvider;
import org.eclipse.cdt.ui.text.c.hover.ICEditorTextHover;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.tcf.internal.cdt.ui.breakpoints.TCFWatchpointTarget;
import org.eclipse.tcf.internal.cdt.ui.commands.TCFPinViewCommand;
import org.eclipse.tcf.internal.cdt.ui.commands.TCFReverseResumeCommand;
import org.eclipse.tcf.internal.cdt.ui.commands.TCFReverseStepIntoCommand;
import org.eclipse.tcf.internal.cdt.ui.commands.TCFReverseStepOverCommand;
import org.eclipse.tcf.internal.cdt.ui.commands.TCFReverseStepReturnCommand;
import org.eclipse.tcf.internal.cdt.ui.commands.TCFReverseToggleCommand;
import org.eclipse.tcf.internal.cdt.ui.commands.TCFStepIntoSelectionHandler;
import org.eclipse.tcf.internal.cdt.ui.disassembly.TCFDisassemblyBackend;
import org.eclipse.tcf.internal.cdt.ui.hover.TCFDebugTextHover;
import org.eclipse.tcf.internal.cdt.ui.sourcelookup.TCFSourceNotFoundPresentation;
import org.eclipse.tcf.internal.debug.ui.model.ISourceNotFoundPresentation;
import org.eclipse.tcf.internal.debug.ui.model.TCFModel;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExpression;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeStackFrame;

@SuppressWarnings({ "rawtypes", "restriction" })
public class TCFNodeAdapterFactory implements IAdapterFactory {

    // Not available before CDT 8.2
    private static final String STEP_INTO_SELECTION =
        "org.eclipse.cdt.debug.core.model.IStepIntoSelectionHandler";

    private static final Object[] class_names = {
        IDisassemblyBackend.class,
        ISteppingModeTarget.class,
        ISuspendResume.class,
        ICEditorTextHover.class,
        IReverseToggleHandler.class,
        IReverseStepIntoHandler.class,
        IReverseStepOverHandler.class,
        IReverseResumeHandler.class,
        STEP_INTO_SELECTION,
        IUncallHandler.class,
        IPinProvider.class,
        ICWatchpointTarget.class,
        ISourceNotFoundPresentation.class
    };

    private static final Class[] class_list;

    static {
        ArrayList<Class> l = new ArrayList<Class>();
        for (Object o : class_names) {
            if (o instanceof Class) {
                l.add((Class)o);
            }
            else {
                try {
                    l.add(Class.forName((String)o));
                }
                catch (ClassNotFoundException e) {
                }
            }
        }
        class_list = l.toArray(new Class[l.size()]);
    }

    public Object getAdapter(Object obj, Class type) {
        if (obj instanceof TCFNode) {
            final TCFNode node = (TCFNode)obj;
            TCFModel model = node.getModel();
            Object handler = model.getAdapter(type, node);
            if (handler == null) {
                if (IDisassemblyBackend.class == type) {
                    TCFDisassemblyBackend backend = new TCFDisassemblyBackend();
                    if (backend.supportsDebugContext(node)) return backend;
                    backend.dispose();
                }
                else if (ISteppingModeTarget.class == type) {
                    model.setAdapter(type, handler = new TCFSteppingModeTarget(model));
                }
                else if (ISuspendResume.class == type) {
                    TCFNodeExecContext exec = null;
                    if (node instanceof TCFNodeExecContext) {
                        exec = (TCFNodeExecContext)node;
                    }
                    else if (node instanceof TCFNodeStackFrame) {
                        exec = (TCFNodeExecContext)node.getParent();
                    }
                    if (exec != null) {
                        return new TCFSuspendResumeAdapter(exec);
                    }
                }
                else if (ICEditorTextHover.class == type) {
                    model.setAdapter(type, handler = new TCFDebugTextHover());
                }
                else if (IReverseToggleHandler.class == type) {
                    model.setAdapter(type, handler = new TCFReverseToggleCommand());
                }
                else if (IReverseStepIntoHandler.class == type) {
                    model.setAdapter(type, handler = new TCFReverseStepIntoCommand(model));
                }
                else if (IReverseStepOverHandler.class == type) {
                    model.setAdapter(type, handler = new TCFReverseStepOverCommand(model));
                }
                else if (IUncallHandler.class == type) {
                    model.setAdapter(type, handler = new TCFReverseStepReturnCommand(model));
                }
                else if (IReverseResumeHandler.class == type) {
                    model.setAdapter(type, handler = new TCFReverseResumeCommand(model));
                }
                else if (IPinProvider.class == type) {
                    model.setAdapter(type, handler = new TCFPinViewCommand(model));
                }
                else if (ICWatchpointTarget.class == type) {
                    if (node instanceof TCFNodeExpression) return new TCFWatchpointTarget((TCFNodeExpression)node);
                }
                else if (ISourceNotFoundPresentation.class == type) {
                    model.setAdapter(type, handler = new TCFSourceNotFoundPresentation());
                }
                else if (type.getName().equals(STEP_INTO_SELECTION)) {
                    model.setAdapter(type, handler = new TCFStepIntoSelectionHandler());
                }
            }
            return handler;
        }
        return null;
    }

    public Class[] getAdapterList() {
        return class_list;
    }
}
