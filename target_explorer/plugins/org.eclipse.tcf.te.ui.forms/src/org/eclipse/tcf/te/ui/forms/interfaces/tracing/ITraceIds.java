/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.forms.interfaces.tracing;

/**
 * Plug-in trace slot identifiers.
 */
public interface ITraceIds {

	/**
	 * If activated, tracing information about the section dirty state is printed out.
	 */
	public static String TRACE_SECTIONS_DIRTY_STATE = "trace/sections/dirtyState"; //$NON-NLS-1$

	/**
	 * If activated, tracing information about the section stale state is printed out.
	 */
	public static String TRACE_SECTIONS_STALE_STATE = "trace/sections/staleState"; //$NON-NLS-1$
}
