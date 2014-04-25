/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper.interfaces;

import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.stepper.job.AbstractStepperJobSchedulingRule;

/**
 * Stepper service.
 */
public interface IStepperService extends IService {

	/**
	 * Get a job scheduling rule for the given context.
	 * @param context The context. Must not be <code>null</code>.
	 * @param operation The operation. Must not be <code>null</code>.
	 * @return The scheduling rule or <code>null</code>.
	 */
	public AbstractStepperJobSchedulingRule getSchedulingRule(Object context, String operation);
}
