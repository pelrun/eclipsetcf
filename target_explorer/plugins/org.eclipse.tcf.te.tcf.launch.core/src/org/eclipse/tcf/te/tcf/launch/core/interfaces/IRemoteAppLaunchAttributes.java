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
 * Defines the launch configuration attribute id's to access the launch step contexts.
 */
public interface IRemoteAppLaunchAttributes {

	/**
	 * Launch configuration attribute: The process image.
	 */
	public static final String ATTR_PROCESS_IMAGE = ICommonTCFLaunchAttributes.ATTR_PREFIX + ".process_image"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute: The process arguments.
	 */
	public static final String ATTR_PROCESS_ARGUMENTS = ICommonTCFLaunchAttributes.ATTR_PREFIX + ".process_arguments"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute: Flag if or if not to stop at process entry.
	 */
	public static final String ATTR_STOP_AT_ENTRY = ICommonTCFLaunchAttributes.ATTR_PREFIX + ".process_stop_at_entry"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute: Flag if or if not to stop at main.
	 */
	public static final String ATTR_STOP_AT_MAIN = ICommonTCFLaunchAttributes.ATTR_PREFIX + ".process_stop_at_main"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute: Flag if or if not to attach process children.
	 */
	public static final String ATTR_ATTACH_CHILDREN = ICommonTCFLaunchAttributes.ATTR_PREFIX + ".process_attach_children"; //$NON-NLS-1$

	/**
	 * Launch configuration attribute (internal use): The process context object.
	 */
	public static final String ATTR_PROCESS_CONTEXT = ICommonTCFLaunchAttributes.ATTR_PREFIX + ".process_context"; //$NON-NLS-1$
}
