/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper.interfaces.tracing;

/**
 * Stepper Runtime plug-in trace slot identifiers.
 */
public interface ITraceIds {

	/**
	 * If activated, trace information about step execution is printed out.
	 */
	public static final String TRACE_STEPPING = "trace/stepping"; //$NON-NLS-1$

	/**
	 * If activated, profile information about step execution is printed out.
	 */
	public static final String PROFILE_STEPPING = "profile/stepping"; //$NON-NLS-1$
}
