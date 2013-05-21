/*******************************************************************************
 * Copyright (c) 2004, 2012 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Ericsson             - DSF-GDB version
 *     Nokia                - Made generic to DSF
 *     Wind River Systems   - Adapted to TCF Debug
 *******************************************************************************/
package org.eclipse.tcf.internal.cdt.ui;

import org.eclipse.cdt.debug.ui.CDebugUIPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextService;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeExecContext;
import org.eclipse.tcf.internal.debug.ui.model.TCFNodeStackFrame;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Manages the current evaluation context (stack frame) for evaluation actions.
 * In each page, the selection is tracked in each debug view (if any). When a
 * debug target selection exists, the "debuggerActive" System property is set to
 * true. This property is used to make the "Run To Line", "Resume At Line",
 * "Move To Line" and "Add Watch Expression" actions visible in editors only if
 * there is a running debug session.
 */
public class EvaluationContextManager implements IWindowListener, IDebugContextListener {

    // Must use the same ID as the base CDT since we want to enable
    // actions that are registered by base CDT.
    private final static String DEBUGGER_ACTIVE = CDebugUIPlugin.PLUGIN_ID + ".debuggerActive"; //$NON-NLS-1$

    protected static EvaluationContextManager fgManager;

    protected EvaluationContextManager() {
    }

    public static void startup() {
        WorkbenchJob job = new WorkbenchJob("") {
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                if (fgManager == null) {
                    fgManager = new EvaluationContextManager();
                    IWorkbench workbench = PlatformUI.getWorkbench();
                    IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
                    if (window != null) {
                        fgManager.windowActivated(window);
                    }
                    workbench.addWindowListener(fgManager);
                }
                return Status.OK_STATUS;
            }
        };
        job.setPriority(Job.SHORT);
        job.setSystem(true);
        job.schedule();
    }


    /* (non-Javadoc)
     * @see org.eclipse.ui.IWindowListener#windowActivated(org.eclipse.ui.IWorkbenchWindow)
     */
    public void windowActivated(IWorkbenchWindow window) {
            IDebugContextService service = DebugUITools.getDebugContextManager().getContextService(window);
            service.addDebugContextListener(this);
            selectionChanged( service.getActiveContext() );
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWindowListener#windowDeactivated(org.eclipse.ui.IWorkbenchWindow)
     */
    public void windowDeactivated(IWorkbenchWindow window) {
             DebugUITools.getDebugContextManager().getContextService(window).removeDebugContextListener(this);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWindowListener#windowOpened(org.eclipse.ui.IWorkbenchWindow)
     */
    public void windowOpened(IWorkbenchWindow window) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWindowListener#windowClosed(org.eclipse.ui.IWorkbenchWindow)
     */
    public void windowClosed(IWorkbenchWindow window) {
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.debug.ui.contexts.IDebugContextListener#debugContextChanged(org.eclipse.debug.ui.contexts.DebugContextEvent)
     */
    public void debugContextChanged(DebugContextEvent event) {
        selectionChanged(event.getContext());
    }

    /*
     * Takes the current selection and validates that we have a valid TCF node
     * selected and various actions should be visible and enabled.
     */
    private void selectionChanged(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection)selection;
            if (ss.size() == 1) {
                Object element = ss.getFirstElement();
                if (element instanceof TCFNodeExecContext || element instanceof TCFNodeStackFrame) {
                    System.setProperty(DEBUGGER_ACTIVE, Boolean.toString(true));
                    return;
                }
            }
        }

        // no context in the given view
        System.setProperty(DEBUGGER_ACTIVE, Boolean.toString(false));
    }
}
