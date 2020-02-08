/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
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
