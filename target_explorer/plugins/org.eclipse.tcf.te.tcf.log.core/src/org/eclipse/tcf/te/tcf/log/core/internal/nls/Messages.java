/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.log.core.internal.nls;

import org.eclipse.osgi.util.NLS;

/**
 * Plug-in externalized strings management.
 */
public class Messages extends NLS {

	// The plug-in resource bundle name
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.tcf.log.core.internal.nls.Messages"; //$NON-NLS-1$

	/**
	 * Static constructor.
	 */
	static {
		// Load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	// **** Declare externalized string id's down here *****

	public static String ChannelTraceListener_channelOpening_message;
	public static String ChannelTraceListener_channelRedirected_message;
	public static String ChannelTraceListener_channelOpened_message;
	public static String ChannelTraceListener_channelClosed_message;
	public static String ChannelTraceListener_channelMark_message;
	public static String ChannelTraceListener_channelServices_message;

	public static String LogManager_error_renameFailed;
}
