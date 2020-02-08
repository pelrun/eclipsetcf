/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.core.interfaces.tracing;

/**
 * Launch core plug-in trace slot identifiers.
 */
public interface ITraceIds {

	/**
	 * If activated, trace information about matching existing launch configurations to a given
	 * launch spec is printed out.
	 */
	public static final String TRACE_LAUNCHCONFIGURATIONMATCHING = "trace/launchConfigurationMatching"; //$NON-NLS-1$
}
