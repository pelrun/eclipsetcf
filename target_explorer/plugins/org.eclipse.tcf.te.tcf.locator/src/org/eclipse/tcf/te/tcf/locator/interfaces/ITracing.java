/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces;

/**
 * TCF locator bundle tracing identifiers.
 */
public interface ITracing {


	/**
	 * If enabled, prints information about peer model method invocations.
	 */
	public static String ID_TRACE_PEER_MODEL = "trace/peerModel"; //$NON-NLS-1$

	/**
	 * If enabled, prints information about locator model method invocations.
	 */
	public static String ID_TRACE_LOCATOR_MODEL = "trace/locatorModel"; //$NON-NLS-1$

	/**
	 * If enabled, prints information about locator listener method invocations.
	 */
	public static String ID_TRACE_LOCATOR_LISTENER = "trace/locatorListener"; //$NON-NLS-1$

	/**
	 * If enabled, prints information about target ping.
	 */
	public static String ID_TRACE_PING = "trace/ping"; //$NON-NLS-1$
}
