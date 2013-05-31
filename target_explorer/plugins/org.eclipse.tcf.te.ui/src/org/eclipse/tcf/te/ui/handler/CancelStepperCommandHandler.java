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
import org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService;
import org.eclipse.tcf.te.runtime.stepper.job.StepperJob;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.EditorPart;

/**
 * Cancel stepper command handler implementation.
 */
public class CancelStepperCommandHandler extends AbstractHandler implements IExecutableExtension {

	private String operation = null;
	private String adaptTo = null;

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
				Object element = iterator.next();
				Object adapted = element;
				if (adaptTo != null) {
					Object adapter = Platform.getAdapterManager().getAdapter(element, adaptTo);
					if (adapter != null) adapted = adapter;
				}
				IPropertiesAccessService service = ServiceManager.getInstance().getService(adapted, IPropertiesAccessService.class);
				StepperJob job = service != null ? (StepperJob)service.getProperty(adapted, StepperJob.class.getName() + "." + operation) : null; //$NON-NLS-1$
				if (service == null && adapted instanceof IPropertiesContainer)
					job = (StepperJob)((IPropertiesContainer)adapted).getProperty(StepperJob.class.getName() + "." + operation); //$NON-NLS-1$
				if (job != null)
					job.cancel();
			}
		}

		return null;
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
