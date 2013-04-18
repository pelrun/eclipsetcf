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

import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperService;

/**
 * AbstractStepperService
 */
public abstract class AbstractStepperService extends AbstractService implements IStepperService {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperService#isCancelable(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isCancelable(Object context, String operation) {
	    return OPERATION_CONNECT.equals(operation);
	}
}
