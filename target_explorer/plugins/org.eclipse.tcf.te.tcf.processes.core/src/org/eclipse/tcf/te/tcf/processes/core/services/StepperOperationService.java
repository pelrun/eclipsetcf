/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.processes.core.services;

import org.eclipse.tcf.te.runtime.stepper.services.AbstractStepperOperationService;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.services.IStepGroupIds;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.services.IStepperServiceOperations;

/**
 * Processes stepper operation service implementation.
 */
public class StepperOperationService extends AbstractStepperOperationService {
	/**
	 * Constructor.
	 */
	public StepperOperationService() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.services.StepperOperationService#isHandledOperation(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isHandledOperation(Object context, String operation) {
		return IStepperServiceOperations.ATTACH.equals(operation) ||
						IStepperServiceOperations.DETACH.equals(operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.services.StepperOperationService#addToActionHistory(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean addToActionHistory(Object context, String operation) {
		return IStepperServiceOperations.ATTACH.equals(operation) ||
						IStepperServiceOperations.DETACH.equals(operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.services.StepperOperationService#getStepGroupId(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getStepGroupId(Object context, String operation) {
		if (IStepperServiceOperations.ATTACH.equals(operation)) {
			return IStepGroupIds.ATTACH;
		}
		if (IStepperServiceOperations.DETACH.equals(operation)) {
			return IStepGroupIds.DETACH;
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.services.StepperOperationService#getStepGroupName(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getStepGroupName(Object context, String operation) {
		if (IStepperServiceOperations.ATTACH.equals(operation)) {
			return "Attach"; //$NON-NLS-1$
		}
		if (IStepperServiceOperations.DETACH.equals(operation)) {
			return "Detach"; //$NON-NLS-1$
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#isEnabled(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isEnabled(Object context, String operation) {
		return IStepperServiceOperations.ATTACH.equals(operation) ||
						IStepperServiceOperations.DETACH.equals(operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.services.StepperOperationService#isCancelable(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isCancelable(Object context, String operation) {
		return IStepperServiceOperations.ATTACH.equals(operation) ||
						IStepperServiceOperations.DETACH.equals(operation);
	}
}
