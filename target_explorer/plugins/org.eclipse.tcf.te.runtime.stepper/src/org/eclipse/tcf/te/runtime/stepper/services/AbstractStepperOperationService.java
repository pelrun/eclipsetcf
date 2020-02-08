/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.runtime.stepper.services;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService;

/**
 * AbstractStepperOperationService
 */
public abstract class AbstractStepperOperationService extends AbstractService implements IStepperOperationService {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#getStepContext(java.lang.Object, java.lang.String)
	 */
	@Override
	public IStepContext getStepContext(Object context, String operation) {
		IStepContext stepContext = null;

		if (isHandledOperation(context, operation)) {
			if (context instanceof IAdaptable) {
				stepContext = (IStepContext)((IAdaptable)context).getAdapter(IStepContext.class);
			}
			if (stepContext == null && context != null) {
				stepContext = (IStepContext)Platform.getAdapterManager().getAdapter(context, IStepContext.class);
			}
		}

		return stepContext;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#validateStepData(java.lang.Object, java.lang.String, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public boolean validateStepData(Object context, String operation, IPropertiesContainer data) {
	    return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#getSpecialHistoryData(java.lang.Object, java.lang.String, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public IPropertiesContainer getSpecialHistoryData(Object context, String operation, IPropertiesContainer data) {
	    return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#getStepGroupData(java.lang.Object, java.lang.String, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public IPropertiesContainer getStepGroupData(Object context, String operation, IPropertiesContainer data) {
	    return data;
	}
}
