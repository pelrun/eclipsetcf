/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces;

/**
 * Step attribute keys.
 */
public interface IStepAttributes {

	/**
	 * Define the prefix used by all other attribute id's as prefix.
	 */
	public static final String ATTR_PREFIX = "org.eclipse.tcf.te.tcf.locator"; //$NON-NLS-1$

	/**
	 * Marker for AttachDebuggerStep if the debugger should be attached or not to the active context.
	 */
	public static final String ATTR_START_DEBUGGER = ATTR_PREFIX + ".start_debugger"; //$NON-NLS-1$

	/**
	 * Marker for StartPingTimerStep if ping should be started on client side.
	 */
	public static final String ATTR_START_CLIENT_PING = ATTR_PREFIX + ".start_client_ping"; //$NON-NLS-1$
}
