/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper.interfaces;

/**
 * Defines common step data attribute id's.
 */
public interface IStepAttributes {

	/**
	 * Define the prefix used by all other attribute id's as prefix.
	 */
	public static final String ATTR_PREFIX = "org.eclipse.tcf.te.runtime.stepper"; //$NON-NLS-1$

	/**
	 * The active context the launch is operating with.
	 */
	public static final String ATTR_ACTIVE_CONTEXT = ATTR_PREFIX + ".active_context"; //$NON-NLS-1$

	/**
	 * The running job.
	 */
	public static final String ATTR_STEPPER_JOB = ATTR_PREFIX + ".stepper_job"; //$NON-NLS-1$

	/**
	 * The running job operation.
	 */
	public static final String ATTR_STEPPER_JOB_OPERATION = ATTR_PREFIX + ".stepper_job_operation"; //$NON-NLS-1$

	/**
	 * The id to persist the data of stepper execution to the history.
	 * If this attribute is not set, it is filled automatically with <stepGroupId>@<stepContextId>
	 */
	public static final String ATTR_HISTORY_ID = ATTR_PREFIX + ".history_id"; //$NON-NLS-1$

	/**
	 * The data to persist to the history.
	 * If this attribute is not set, it is filled automatically with the given stepper data.
	 */
	public static final String ATTR_HISTORY_DATA = ATTR_PREFIX + ".history_data"; //$NON-NLS-1$

	/**
	 * The number of entries in the history.
	 * If this attribute is not set, the history manager default is used.
	 */
	public static final String ATTR_HISTORY_COUNT = ATTR_PREFIX + ".history_count"; //$NON-NLS-1$
}
