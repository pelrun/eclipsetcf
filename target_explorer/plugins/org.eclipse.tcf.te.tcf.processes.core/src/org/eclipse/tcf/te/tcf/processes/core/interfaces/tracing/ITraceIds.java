/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.interfaces.tracing;

/**
 * Core plug-in trace slot identifiers.
 */
public interface ITraceIds {

	/**
	 * If activated, tracing information about the remote process launcher is printed out.
	 */
	public static final String TRACE_PROCESS_LAUNCHER = "trace/launcher/processLauncher"; //$NON-NLS-1$

	/**
	 * If activated, tracing information about the remote processes listener is printed out.
	 */
	public static final String TRACE_PROCESSES_LISTENER = "trace/launcher/processesListener"; //$NON-NLS-1$

	/**
	 * If activated, tracing information about the remote processes streams listener is printed out.
	 */
	public static final String TRACE_STREAMS_LISTENER = "trace/launcher/streamsListener"; //$NON-NLS-1$

	/**
	 * If activated, trace information about the process service listener invocation is printed out.
	 */
	public static final String TRACE_SERVICE_LISTENER = "trace/service/listener"; //$NON-NLS-1$
}
