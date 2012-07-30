/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.core.interfaces;

/**
 * Defines the launch configuration attribute id's for attach launches.
 */
public interface IAttachLaunchAttributes {

	/**
	 * Launch configuration attribute: List of TCF services to use to attach all their children (i.e. {IProcesses})
	 */
	public static final String ATTR_ATTACH_SERVICES = ICommonTCFLaunchAttributes.ATTR_PREFIX + ".attach_services"; //$NON-NLS-1$
}
