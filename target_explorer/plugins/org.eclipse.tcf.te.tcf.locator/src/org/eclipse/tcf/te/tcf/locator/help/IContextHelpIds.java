/**
 * IContextHelpIds.java
 * Created on Feb 13, 2013
 *
 * Copyright (c) 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.locator.help;

import org.eclipse.tcf.te.tcf.locator.activator.CoreBundleActivator;


/**
 * Context help id definitions.
 */
public interface IContextHelpIds {

	/**
	 * UI plug-in common context help id prefix.
	 */
	public final static String PREFIX = CoreBundleActivator.getUniqueIdentifier() + "."; //$NON-NLS-1$

	/**
	 * Simulator utilities: Simulator start failed.
	 */
	public final static String MESSAGE_SIM_START_FAILED = PREFIX + ".status.messageSimStartFailed"; //$NON-NLS-1$

	/**
	 * Simulator utilities: Simulator stop failed.
	 */
	public final static String MESSAGE_SIM_STOP_FAILED = PREFIX + ".status.messageSimStopFailed"; //$NON-NLS-1$
}
