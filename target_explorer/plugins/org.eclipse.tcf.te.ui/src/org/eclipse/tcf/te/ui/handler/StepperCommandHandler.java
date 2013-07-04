/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.handler;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperService;
import org.eclipse.tcf.te.runtime.stepper.job.StepperJob;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.EditorPart;

/**
 * Stepper command handler implementation.
 */
public class StepperCommandHandler extends AbstractHandler implements IExecutableExtension {

	protected String operation = null;
	protected String adaptTo = null;

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.ui.handler.AbstractAgentCommandHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Assert.isNotNull(operation);

		IStructuredSelection selection = getSelection(event);

		Iterator<?> iterator = selection.iterator();
		while (iterator.hasNext()) {
			Object element = iterator.next();
			Object adapted = element;
			if (adaptTo != null) {
				Object adapter = Platform.getAdapterManager().getAdapter(element, adaptTo);
				if (adapter != null) adapted = adapter;
			}
			IStepperService stepperService = getStepperService(adapted, operation);
			if (stepperService != null) {
				IStepContext stepContext = stepperService.getStepContext(adapted, operation);
				String stepGroupId = stepperService.getStepGroupId(adapted, operation);
				String name = stepperService.getStepGroupName(adapted, operation);
				boolean isCancelable = stepperService.isCancelable(adapted, operation);
				IPropertiesContainer data = stepperService.getStepData(adapted, operation);

				if (stepGroupId != null && stepContext != null) {
					scheduleStepperJob(stepContext, data, stepGroupId, name, isCancelable);
				}
			}
		}

		return null;
	}

	/**
	 * Get the stepper service for the goven context and operation.
	 * @param context The context.
	 * @param operation The operation.
	 * @return The stepper service or <code>null</code>.
	 */
	protected IStepperService getStepperService(Object context, String operation) {
		IService[] services = ServiceManager.getInstance().getServices(context, IStepperService.class, false);
		IStepperService stepperService = null;
		for (IService service : services) {
			if (service instanceof IStepperService && ((IStepperService)service).isHandledOperation(context, operation)) {
				stepperService = (IStepperService)service;
			}
        }
		return stepperService;
	}

	/**
	 * Get the selection for the handler execution.
	 * @param event The event.
	 * @return The selection.
	 */
	protected IStructuredSelection getSelection(ExecutionEvent event) {
		// Get the active part
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		// Get the current selection
		ISelection selection = HandlerUtil.getCurrentSelection(event);

		// If the handler is invoked from an editor part, ignore the selection and
		// construct an artificial selection from the active editor input.
		if (part instanceof EditorPart) {
			IEditorInput input = ((EditorPart)part).getEditorInput();
			Object element = input != null ? input.getAdapter(Object.class) : null;
			if (element != null) {
				selection = new StructuredSelection(element);
			}
		}

		return (selection instanceof IStructuredSelection && !selection.isEmpty()) ? (IStructuredSelection)selection : new StructuredSelection();
	}

	/**
	 * Schedule the stepper job.
	 * @param stepContext The step context.
	 * @param data The execution data.
	 * @param stepGroupId The step group id to execute.
	 * @param name The job name.
	 * @param isCancelable <code>true</code> if the job should be cancelable.
	 */
	protected void scheduleStepperJob(IStepContext stepContext, IPropertiesContainer data, String stepGroupId, String name, boolean isCancelable) {
		StepperJob job = new StepperJob(name != null ? name : "", //$NON-NLS-1$
				stepContext,
				data,
				stepGroupId,
				operation,
				isCancelable);
		job.schedule();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		if (data instanceof Map) {
			Map<?,?> dataMap = (Map<?,?>)data;
			if (dataMap.get("operation") instanceof String) { //$NON-NLS-1$
				this.operation = dataMap.get("operation").toString(); //$NON-NLS-1$
			}
			if (dataMap.get("adaptTo") instanceof String) { //$NON-NLS-1$
				this.adaptTo = dataMap.get("adaptTo").toString(); //$NON-NLS-1$
			}
		}
	}
}
