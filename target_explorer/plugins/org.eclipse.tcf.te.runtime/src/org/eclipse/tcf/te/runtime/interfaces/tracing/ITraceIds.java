/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.interfaces.tracing;

/**
 * Runtime plug-in trace slot identifiers.
 */
public interface ITraceIds {

	/**
	 * If activated, trace information about event dispatching is printed out.
	 */
	public static final String TRACE_EVENTS = "trace/events"; //$NON-NLS-1$

	/**
	 * If activated, trace information about asynchronous callbacks is printed out.
	 */
	public static final String TRACE_CALLBACKS = "trace/callbacks"; //$NON-NLS-1$
}
