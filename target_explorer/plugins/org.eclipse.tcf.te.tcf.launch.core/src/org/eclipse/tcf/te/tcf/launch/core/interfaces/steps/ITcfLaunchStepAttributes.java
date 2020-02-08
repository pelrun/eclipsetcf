/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.core.interfaces.steps;

/**
 * Defines the launch configuration attribute id's for attach launches.
 */
public interface ITcfLaunchStepAttributes {

	/**
	 * Define the prefix used by all other attribute id's as prefix.
	 */
	public static final String ATTR_PREFIX = "org.eclipse.tcf.te.tcf.launch"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute: List of TCF services to use to attach all their children (i.e. {IProcesses})
	 */
	public static final String ATTR_ATTACH_SERVICES = ATTR_PREFIX + ".attach_services"; //$NON-NLS-1$
}
