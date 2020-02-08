/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService;
import org.eclipse.tcf.te.runtime.stepper.utils.StepperHelper;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.EditorPart;

/**
 * Stepper command handler implementation.
 */
public abstract class AbstractStepperCommandHandler extends AbstractCommandHandler implements IExecutableExtension {

	protected String operation = null;
	protected String adaptTo = null;

	/**
	 * Part id: Project Explorer view
	 */
	public static final String PART_ID_PROJECT_VIEW = "org.eclipse.ui.navigator.ProjectExplorer"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IPropertiesContainer data = getData(event);
		if (data == null) {
			return null;
		}

		Object context = getContext(data);

		IStepperOperationService stepperOperationService = StepperHelper.getService(context, operation);
		if (stepperOperationService != null) {
			StepperHelper.scheduleStepperJob(context, operation, stepperOperationService, cleanupData(data), null, null);
		}

		return null;
	}

	/**
	 * Get the data from dialog or history.
	 * @param event
	 * @return
	 */
	abstract protected IPropertiesContainer getData(ExecutionEvent event);

	/**
	 * Get the context from the data.
	 * @param data
	 * @return
	 */
	abstract protected Object getContext(IPropertiesContainer data);

	/**
	 * Cleanup the stepper data.
	 * I.e. remove temporary properties that are not needed for the stepper and should also not stored in the stepper history.
	 * @param data
	 * @return
	 */
	protected IPropertiesContainer cleanupData(IPropertiesContainer data) {
	    return data;
	}

	/**
	 * Get the selection for the handler execution.
	 *
	 * @param event The event.
	 * @return The selection.
	 */
	@Override
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
