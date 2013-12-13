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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService;
import org.eclipse.tcf.te.runtime.stepper.job.StepperJob;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.ui.activator.UIPlugin;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.EditorPart;

/**
 * Stepper command handler implementation.
 */
public abstract class AbstractStepperCommandHandler extends AbstractEditorCommandHandler {

	protected String operation = null;
	protected String adaptTo = null;

	/**
	 * Part id: Project Explorer view
	 */
	public static final String PART_ID_PROJECT_VIEW = "org.eclipse.ui.navigator.ProjectExplorer"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.handler.AbstractEditorCommandHandler#internalExecute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected Object internalExecute(ExecutionEvent event) throws ExecutionException {
		Assert.isNotNull(operation);

		IPropertiesContainer data = getData(event);
		if (data == null) {
			return null;
		}

		Object context = getContext(event, data);

		IStepperOperationService stepperOperationService = getStepperService(context, operation);
		if (stepperOperationService != null) {
			IStepContext stepContext = stepperOperationService.getStepContext(context, operation);
			String stepGroupId = stepperOperationService.getStepGroupId(context, operation);
			String name = stepperOperationService.getStepGroupName(context, operation);
			boolean isCancelable = stepperOperationService.isCancelable(context, operation);

			if (stepGroupId != null && stepContext != null) {
				scheduleStepperJob(stepContext, data, stepGroupId, name, isCancelable);
			}
		}

		return null;
	}

	abstract protected IPropertiesContainer getData(ExecutionEvent event);

	abstract protected Object getContext(ExecutionEvent event, IPropertiesContainer data);

	/**
	 * Get the stepper service for the given context and operation.
	 *
	 * @param context The context.
	 * @param operation The operation.
	 * @return The stepper service or <code>null</code>.
	 */
	protected IStepperOperationService getStepperService(Object context, String operation) {
		IService[] services = ServiceManager.getInstance().getServices(context, IStepperOperationService.class, false);
		IStepperOperationService stepperOperationService = null;
		for (IService service : services) {
			if (service instanceof IStepperOperationService && ((IStepperOperationService)service).isHandledOperation(context, operation)) {
				stepperOperationService = (IStepperOperationService)service;
				break;
			}
        }
		return stepperOperationService;
	}

	/**
	 * Get the selection for the handler execution.
	 *
	 * @param event The event.
	 * @return The selection.
	 */
	protected IStructuredSelection getSelection(ExecutionEvent event) {
		// Get the current selection
		ISelection selection = HandlerUtil.getCurrentSelection(event);

		List<Object> elements = new ArrayList<Object>();
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Iterator<Object> iterator = ((IStructuredSelection)selection).iterator();
			while (iterator.hasNext()) elements.add(iterator.next());
		}

		// Get the active part
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		// If the handler is invoked from an editor part, construct an artificial selection
		// from the active editor input.
		if (part instanceof EditorPart) {
			IEditorInput input = ((EditorPart)part).getEditorInput();
			Object element = input != null ? input.getAdapter(Object.class) : null;
			if (element != null && !elements.contains(element)) elements.add(element);
		}

		selection = elements.isEmpty() ? new StructuredSelection() : new StructuredSelection(elements);

		return (IStructuredSelection)selection;
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
		try {
			StepperJob job = new StepperJob(name != null ? name : "", //$NON-NLS-1$
											stepContext,
											data,
											stepGroupId,
											operation,
											isCancelable,
											true);
			job.schedule();
		} catch (IllegalStateException e) {
			if (Platform.inDebugMode()) {
				UIPlugin.getDefault().getLog().log(StatusHelper.getStatus(e));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		super.setInitializationData(config, propertyName, data);
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

	public static IStructuredSelection getPartSelection(String partId) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (partId != null && window != null && window.getActivePage() != null) {
			ISelection sel = window.getActivePage().getSelection(partId);

			if (sel instanceof IStructuredSelection) {
				return (IStructuredSelection)sel;
			}
		}
		return null;
	}

	public static IStructuredSelection getEditorInputSelection() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null && window.getActivePage() != null && window.getActivePage().getActiveEditor() != null) {
			return new StructuredSelection(window.getActivePage().getActiveEditor().getEditorInput());
		}
		return null;
	}
}
