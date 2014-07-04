/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.runtime.stepper.utils;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.persistence.utils.DataHelper;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepAttributes;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService;
import org.eclipse.tcf.te.runtime.stepper.job.StepperJob;

/**
 * StepperHelper
 */
public final class StepperHelper {

	public static final IStepperOperationService getService(Object context, String operation) {
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

	public static final void scheduleStepperJob(Object context, String operation, IStepperOperationService service, IPropertiesContainer data, ICallback callback, IProgressMonitor monitor) {
		IStepContext stepContext = service.getStepContext(context, operation);
		String stepGroupId = service.getStepGroupId(context, operation);
		String name = service.getStepGroupName(context, operation);
		boolean isCancelable = service.isCancelable(context, operation);
		boolean addToActionHistory = service.addToActionHistory(context, operation);
		IPropertiesContainer histData = service.getSpecialHistoryData(stepContext, operation, data);
		if (!addToActionHistory) {
			data.setProperty(IStepAttributes.PROP_SKIP_LAST_RUN_HISTORY, true);
		}
		if (histData != null) {
			data.setProperty(IStepAttributes.ATTR_HISTORY_DATA, DataHelper.encodePropertiesContainer(histData));
		}

		if (stepGroupId != null && stepContext != null) {
			scheduleStepperJob(stepContext, stepGroupId, name, histData, isCancelable, callback, monitor);
			StepperJob job = new StepperJob(name != null ? name : "", //$NON-NLS-1$
											stepContext,
											data,
											stepGroupId,
											operation,
											isCancelable,
											monitor == null);
			job.setJobCallback(callback);

			if (monitor != null) {
				job.run(monitor);
			}
			else {
				job.schedule();
			}
		}

	}

	public static final void scheduleStepperJob(Object context, String stepGroupId, String jobName, IPropertiesContainer data, boolean isCancelable, ICallback callback, IProgressMonitor monitor) {
		IStepContext stepContext = null;
		if (context instanceof IStepContext) {
			stepContext = (IStepContext)context;
		}
		else if (context instanceof IAdaptable) {
			stepContext = (IStepContext)((IAdaptable)context).getAdapter(IStepContext.class);
		}
		if (stepContext == null && context != null) {
			stepContext = (IStepContext)Platform.getAdapterManager().getAdapter(context, IStepContext.class);
		}

		StepperJob job = new StepperJob(jobName != null ? jobName : "", //$NON-NLS-1$
						stepContext,
						data,
						stepGroupId,
						stepGroupId,
						isCancelable,
						monitor == null);
		job.setJobCallback(callback);

		if (monitor != null) {
			job.run(monitor);
		}
		else {
			job.schedule();
		}
	}
}
