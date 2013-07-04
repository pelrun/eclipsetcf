/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.runtime.stepper.services;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperService;

/**
 * AbstractStepperService
 */
public abstract class AbstractStepperService extends AbstractService implements IStepperService {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperService#getStepContext(java.lang.Object, java.lang.String)
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
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperService#getStepData(java.lang.Object, java.lang.String)
	 */
	@Override
	public IPropertiesContainer getStepData(Object context, String operation) {
	    return new PropertiesContainer();
	}
}
