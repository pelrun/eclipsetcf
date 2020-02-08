/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.core.lm.interfaces;

/**
 * Defines the common attribute id's used to access launch configuration properties.
 */
public interface ICommonLaunchAttributes {

	/**
	 * Define the prefix used by all other attribute id's as prefix.
	 */
	public static final String ATTR_PREFIX = "org.eclipse.tcf.te.launch"; //$NON-NLS-1$

	/**
	 * Unique identifier.
	 */
	public static final String ATTR_UUID = ATTR_PREFIX + ".UUID";     //$NON-NLS-1$

	/**
	 * Time stamp when last launched.
	 */
	public static final String ATTR_LAST_LAUNCHED = ATTR_PREFIX + ".lastLaunched";     //$NON-NLS-1$

	/**
	 * Attribute used exclusively with <code>ILaunch.setAttribute</code> to mark when
	 * then launch sequence finished. The attribute does not tell if an error occurred
	 * during the launch!
	 */
	public static final String ILAUNCH_ATTRIBUTE_LAUNCH_SEQUENCE_COMPLETED = "launchSequenceCompleted"; //$NON-NLS-1$
}



