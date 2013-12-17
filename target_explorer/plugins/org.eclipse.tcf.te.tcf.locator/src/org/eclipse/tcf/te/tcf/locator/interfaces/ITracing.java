/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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
	 * If enabled, prints information about locator model property tester invocations.
	 */
	public static String ID_TRACE_PROPERTY_TESTER = "trace/propertyTester"; //$NON-NLS-1$
}
