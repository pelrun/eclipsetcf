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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
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

	private String operation = null;

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.ui.handler.AbstractAgentCommandHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Assert.isNotNull(operation);

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

		// If the selection is not empty, iterate over the selection and execute
		// the operation for each peer model node in the selection.
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Iterator<?> iterator = ((IStructuredSelection)selection).iterator();
			while (iterator.hasNext()) {
				final Object element = iterator.next();
				IStepperService service = ServiceManager.getInstance().getService(element, IStepperService.class);
				if (service != null) {
					String stepGroupId = service.getStepGroupId(element, operation);
					IStepContext stepContext = service.getStepContext(element, operation);
					String name = service.getStepGroupName(element, operation);

					if (stepGroupId != null && stepContext != null) {
						IPropertiesContainer data = new PropertiesContainer();
						StepperJob job = new StepperJob(name != null ? name : "", //$NON-NLS-1$
										stepContext,
										data,
										stepGroupId,
										operation);

						job.schedule();
					}
				}
			}
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		if (data instanceof Map && ((Map<?,?>)data).get("operation") instanceof String) { //$NON-NLS-1$
			this.operation = ((Map<?,?>)data).get("operation").toString(); //$NON-NLS-1$
		}
	}
}
