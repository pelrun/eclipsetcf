/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper.internal;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService;
import org.eclipse.tcf.te.runtime.stepper.job.StepperJob;



/**
 * Services plug-in property tester implementation.
 */
public class PropertyTester extends org.eclipse.core.expressions.PropertyTester {

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

		String operation = expectedValue instanceof String ? (String)expectedValue : null;

		if ("isRunning".equals(property)) { //$NON-NLS-1$
			if (operation != null) {
				Map<String,List<Job>> jobs = StepperJob.getJobs(receiver);
				if (jobs != null && jobs.containsKey(operation)) {
					for (Job job : jobs.get(operation)) {
						if (job instanceof StepperJob && !((StepperJob)job).isCanceled() && !((StepperJob)job).isFinished()) {
							return true;
						}
					}
				}
			}
		}

		if ("isRunningOrCanceled".equals(property)) { //$NON-NLS-1$
			if (operation != null) {
				Map<String,List<Job>> jobs = StepperJob.getJobs(receiver);
				if (jobs != null && jobs.containsKey(operation)) {
					for (Job job : jobs.get(operation)) {
						if (job instanceof StepperJob && !((StepperJob)job).isFinished()) {
							return true;
						}
					}
				}
			}
		}

		if ("isEnabled".equals(property)) { //$NON-NLS-1$
			if (operation != null) {
				IService[] services = ServiceManager.getInstance().getServices(receiver, IStepperOperationService.class, false);
				IStepperOperationService stepperOperationService = null;
				for (IService service : services) {
					if (service instanceof IStepperOperationService && ((IStepperOperationService)service).isHandledOperation(receiver, operation)) {
						stepperOperationService = (IStepperOperationService)service;
						break;
					}
	            }
				if (stepperOperationService != null) {
					return stepperOperationService.isEnabled(receiver, operation);
				}
			}
		}

		return false;
	}
}
