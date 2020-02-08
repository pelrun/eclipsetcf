/*******************************************************************************
 * Copyright (c) 2011 - 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.nls;

import java.lang.reflect.Field;

import org.eclipse.osgi.util.NLS;

/**
 * Plug-in externalized strings management.
 */
public class Messages extends NLS {

	// The plug-in resource bundle name
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.tcf.core.nls.Messages"; //$NON-NLS-1$

	/**
	 * Static constructor.
	 */
	static {
		// Load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	/**
	 * Returns if or if not this NLS manager contains a constant for
	 * the given externalized strings key.
	 *
	 * @param key The externalized strings key or <code>null</code>.
	 * @return <code>True</code> if a constant for the given key exists, <code>false</code> otherwise.
	 */
	public static boolean hasString(String key) {
		if (key != null) {
			try {
				Field field = Messages.class.getDeclaredField(key);
				return field != null;
			} catch (NoSuchFieldException e) { /* ignored on purpose */ }
		}

		return false;
	}

	/**
	 * Returns the corresponding string for the given externalized strings
	 * key or <code>null</code> if the key does not exist.
	 *
	 * @param key The externalized strings key or <code>null</code>.
	 * @return The corresponding string or <code>null</code>.
	 */
	public static String getString(String key) {
		if (key != null) {
			try {
				Field field = Messages.class.getDeclaredField(key);
				return (String)field.get(null);
			} catch (Exception e) { /* ignored on purpose */ }
		}

		return null;
	}

	// **** Declare externalized string id's down here *****

	public static String ChannelManager_openChannel_message;
	public static String ChannelManager_openChannel_reuse_message;
	public static String ChannelManager_openChannel_pending_message;
	public static String ChannelManager_openChannel_new_message;
	public static String ChannelManager_openChannel_success_message;
	public static String ChannelManager_openChannel_failed_message;
	public static String ChannelManager_openChannel_failed;
	public static String ChannelManager_closeChannel_close_message;
	public static String ChannelManager_closeChannel_message;
	public static String ChannelManager_closeChannel_inuse_message;
	public static String ChannelManager_closeChannel_closed_message;
	public static String ChannelManager_closeChannel_pending_message;
	public static String ChannelManager_closeChannel_failed_message;
	public static String ChannelManager_stream_closed_message;
	public static String ChannelManager_stream_missing_service_message;

	public static String AbstractExternalValueAdd_error_invalidLocation;
	public static String AbstractExternalValueAdd_start_at;
	public static String AbstractExternalValueAdd_died_at;
	public static String AbstractExternalValueAdd_running_at;
	public static String AbstractExternalValueAdd_start_waiting_at;
	public static String AbstractExternalValueAdd_stop_waiting_at;
	public static String AbstractExternalValueAdd_output;
	public static String AbstractExternalValueAdd_error_cause;
	public static String AbstractExternalValueAdd_error_processDied;
	public static String AbstractExternalValueAdd_error_failedToReadOutput;
	public static String AbstractExternalValueAdd_error_output;
	public static String AbstractExternalValueAdd_error_invalidPeerAttributes;

	public static String ValueAddLauncher_launch_command;

	public static String CallbackMonitor_AllTasksFinished;

	public static String MonitorTask_TimeoutError;

	public static String Extension_error_invalidProtocolStateChangeListener;

	public static String AbstractJob_error_dialogTitle;

	public static String StepperOperationService_stepGroupName_openChannel;
	public static String StepperOperationService_stepGroupName_closeChannel;
}
