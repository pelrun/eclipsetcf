/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.handler;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.tcf.core.model.interfaces.IModel;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelUpdateService;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.steps.TerminateStep;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * The handler to terminate the selected process.
 */
public class TerminateHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Iterator<?> iterator = ((IStructuredSelection)selection).iterator();
			while (iterator.hasNext()) {
				Object candidate = iterator.next();
				if (candidate instanceof IProcessContextNode) {
					final IProcessContextNode process = (IProcessContextNode)candidate;
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							TerminateStep step = new TerminateStep();
							step.executeTerminate(process, new Callback() {
								@Override
								protected void internalDone(Object caller, final IStatus status) {
									if (status.isOK()) {
										IModel model = process.getParent(IModel.class);
										Assert.isNotNull(model);
										model.getService(IModelUpdateService.class).remove(process);
									}
									else {
										PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable(){
											@Override
						                    public void run() {
												String message = status.getMessage();
												Shell parent = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
												MessageDialog.openError(parent, Messages.TerminateHandler_TerminationError, message);
						                    }});
									}
								}
							});
						}
					};
					Protocol.invokeLater(runnable);
				}
			}
		}

		return null;
	}

}
