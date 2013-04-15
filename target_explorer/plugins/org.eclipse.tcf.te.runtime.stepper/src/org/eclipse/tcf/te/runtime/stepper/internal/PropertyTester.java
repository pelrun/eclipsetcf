/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper.internal;

import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperService;
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
			if (operation != null && receiver instanceof IPropertiesContainer) {
				IPropertiesAccessService service = ServiceManager.getInstance().getService(receiver, IPropertiesAccessService.class);
				StepperJob job = service != null ? (StepperJob)service.getProperty(receiver, StepperJob.class.getName() + "." + operation) : null; //$NON-NLS-1$
				if (service == null && receiver instanceof IPropertiesContainer)
					job = (StepperJob)((IPropertiesContainer)receiver).getProperty(StepperJob.class.getName() + "." + operation); //$NON-NLS-1$
				return job != null && !job.isCanceled() && !job.isFinished();
			}
		}

		if ("isRunningOrCanceled".equals(property)) { //$NON-NLS-1$
			if (operation != null && receiver instanceof IPropertiesContainer) {
				IPropertiesAccessService service = ServiceManager.getInstance().getService(receiver, IPropertiesAccessService.class);
				StepperJob job = service != null ? (StepperJob)service.getProperty(receiver, StepperJob.class.getName() + "." + operation) : null; //$NON-NLS-1$
				if (service == null && receiver instanceof IPropertiesContainer)
					job = (StepperJob)((IPropertiesContainer)receiver).getProperty(StepperJob.class.getName() + "." + operation); //$NON-NLS-1$
				return job != null && !job.isFinished();
			}
		}

		if ("isEnabled".equals(property)) { //$NON-NLS-1$
			if (operation != null) {
				IStepperService service = ServiceManager.getInstance().getService(receiver, IStepperService.class);
				if (service != null) {
					return service.isEnabled(receiver, operation);
				}
			}
		}

		return false;
	}
}
