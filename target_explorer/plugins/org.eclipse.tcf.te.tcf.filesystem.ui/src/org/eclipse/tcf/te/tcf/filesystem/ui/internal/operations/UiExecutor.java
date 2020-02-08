/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IOperation;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.ui.PlatformUI;

/**
 * The operation that is executed in an interactive progress dialog.
 */
public class UiExecutor {
    public static IStatus execute(final IOperation operation) {
		final Display display = Display.getCurrent();
		Assert.isNotNull(display);
		final Shell parent = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    	final ProgressMonitorDialog dlg = new ProgressMonitorDialog(parent);
    	dlg.setOpenOnRun(false);

    	display.timerExec(500, new Runnable() {
			@Override
			public void run() {
				Shell shell = dlg.getShell();
				if (shell != null && !shell.isDisposed()) {
					Shell activeShell = display.getActiveShell();
					if (activeShell == null || activeShell == parent) {
						dlg.open();
					} else {
						display.timerExec(500, this);
					}
				}
			}
		});
    	final AtomicReference<IStatus> ref = new AtomicReference<IStatus>();
    	try {
	        dlg.run(true, true, new IRunnableWithProgress() {
	        	@Override
	        	public void run(IProgressMonitor monitor) {
	        		ref.set(operation.run(monitor));
	        	}
	        });
        } catch (InvocationTargetException e) {
        	ref.set(StatusHelper.getStatus(e.getTargetException()));
        } catch (InterruptedException e) {
        	return Status.CANCEL_STATUS;
        }
    	IStatus status = ref.get();
    	if (!status.isOK() && status.getMessage().length() > 0) {
    		ErrorDialog.openError(parent, operation.getName(), Messages.UiExecutor_errorRunningOperation, status);
    		UIPlugin.getDefault().getLog().log(status);
    	}
        return status;
	}
}
