/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
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
	 * The initial step group.
	 */
	public static final String ATTR_STEP_GROUP_ID = ATTR_PREFIX + ".step_group_id"; //$NON-NLS-1$

	/**
	 * The data to persist to the history.
	 * If this attribute is not set, it is filled automatically with the given stepper data.
	 */
	public static final String ATTR_HISTORY_DATA = ATTR_PREFIX + ".history_data"; //$NON-NLS-1$

	/**
	 * Marker for stepper data to not add this run into the laus run history.
	 */
	public static final String PROP_SKIP_LAST_RUN_HISTORY = ATTR_PREFIX + ".skip_last_run_history"; //$NON-NLS-1$

	/**
	 * History id used by the stepper to remember the last run.
	 */
	public static final String PROP_LAST_RUN_HISTORY_ID = ATTR_PREFIX + ".last_run_history_id"; //$NON-NLS-1$

}
