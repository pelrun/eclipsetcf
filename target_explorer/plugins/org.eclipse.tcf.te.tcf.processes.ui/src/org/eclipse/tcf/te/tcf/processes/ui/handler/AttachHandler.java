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
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.steps.AttachStep;
import org.eclipse.tcf.te.tcf.processes.ui.activator.UIPlugin;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * Attach to process command handler implementation.
 */
public class AttachHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Iterator<?> iterator = ((IStructuredSelection)selection).iterator();
			while (iterator.hasNext()) {
				final Object candidate = iterator.next();
				if (candidate instanceof IProcessContextNode) {
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							IProcessContextNode node = (IProcessContextNode)candidate;
							doAttach(event, node);
						}
					};

					Protocol.invokeLater(runnable);
				}
			}
		}

		return null;
	}

	/**
	 * Executes the attach to the given node.
	 * <p>
	 * <b>Note:</b> This method must be called from within the TCF dispatch thread.
	 *
	 * @param event The execution event.
	 * @param node The context to attach. Must not be <code>null</code>.
	 */
	protected void doAttach(final ExecutionEvent event, final IProcessContextNode node) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(node);

		AttachStep step = new AttachStep();
		step.executeAttach(node, new Callback() {
			@Override
			protected void internalDone(Object caller, IStatus status) {
				if (status.getSeverity() == IStatus.OK) {
					// Get the launch instance from the callback properties
					Object launch = getProperty("launch"); //$NON-NLS-1$
					if (launch != null) {
						ICommandService service = (ICommandService)PlatformUI.getWorkbench().getService(ICommandService.class);
						Command command = service != null ? service.getCommand("org.eclipse.tcf.te.launch.command.showInDebugView") : null; //$NON-NLS-1$
						if (command != null && command.isDefined() && command.isEnabled()) {
							try {
								IHandlerService handlerSvc = (IHandlerService)PlatformUI.getWorkbench().getService(IHandlerService.class);
								Assert.isNotNull(handlerSvc);

								ISelection selection = HandlerUtil.getCurrentSelection(event);
								EvaluationContext ctx = new EvaluationContext(handlerSvc.getCurrentState(), selection);
								ctx.addVariable("launch", launch); //$NON-NLS-1$
								ctx.addVariable(ISources.ACTIVE_CURRENT_SELECTION_NAME, selection);
								ctx.addVariable(ISources.ACTIVE_MENU_SELECTION_NAME, selection);
								ctx.setAllowPluginActivation(true);

								ParameterizedCommand pCmd = ParameterizedCommand.generateCommand(command, null);
								Assert.isNotNull(pCmd);

								handlerSvc.executeCommandInContext(pCmd, null, ctx);
							} catch (Exception e) {
								// If the platform is in debug mode, we print the exception to the log view
								if (Platform.inDebugMode()) {
									status = new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(), e.getMessage(), e);
									UIPlugin.getDefault().getLog().log(status);
								}
							}
						}

					}
				}
			}
		});
	}
}
