/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.interfaces.tracing;

/**
 * TCF core plug-in trace slot identifiers.
 */
public interface ITraceIds {

	/**
	 * If activated, tracing information about channel open/close is printed out.
	 */
	public static String TRACE_CHANNELS = "trace/channels"; //$NON-NLS-1$

	/**
	 * If activated, tracing information about the channel manager is printed out.
	 */
	public static String TRACE_CHANNEL_MANAGER = "trace/channelManager"; //$NON-NLS-1$

	/**
	 * If activated, tracing information about the channel manager streams listener proxies are printed out.
	 */
	public static final String TRACE_STREAMS_LISTENER_PROXY = "trace/channelManager/streamsListenerProxy"; //$NON-NLS-1$

}
