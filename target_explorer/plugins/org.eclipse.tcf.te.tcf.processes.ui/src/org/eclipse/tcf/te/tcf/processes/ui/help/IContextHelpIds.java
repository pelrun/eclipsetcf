/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.help;

import org.eclipse.tcf.te.tcf.processes.ui.activator.UIPlugin;

/**
 * Plugin context help id definitions.
 */
public interface IContextHelpIds {

	/**
	 * Common context help id prefix.
	 */
	public final static String PREFIX = UIPlugin.getUniqueIdentifier() + "."; //$NON-NLS-1$

	/**
	 * Error dialog: execution failure in TcfDiscoveryAbstractChannelCommandHandler
	 */
	public final static String CHANNEL_COMMAND_HANDLER_STATUS_DIALOG = PREFIX + "ChannelCommandHandlerStatusDialog"; //$NON-NLS-1$

	/**
	 * Target Explorer details editor page: Process explorer
	 */
	public final static String PROCESS_EXPLORER_EDITOR_PAGE = PREFIX + "ProcessExplorerEditorPage"; //$NON-NLS-1$

	/**
	 * Launch object dialog.
	 */
	public final static String LAUNCH_OBJECT_DIALOG = PREFIX + "LaunchObjectDialog"; //$NON-NLS-1$

	/**
	 * Error dialog: remote process launch failed.
	 */
	public final static String LAUNCH_PROCESS_ERROR_DIALOG = PREFIX + "LaunchProcessErrorDialog"; //$NON-NLS-1$

	/**
	 * Terminate command handler: Terminate operation failed.
	 */
	public final static String MESSAGE_TERMINATE_FAILED = PREFIX + ".status.messageTerminateFailed"; //$NON-NLS-1$
}
