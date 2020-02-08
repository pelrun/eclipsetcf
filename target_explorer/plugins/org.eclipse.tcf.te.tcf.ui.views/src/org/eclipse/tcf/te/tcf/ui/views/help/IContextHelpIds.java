/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.views.help;

import org.eclipse.tcf.te.tcf.ui.views.activator.UIPlugin;

/**
 * UI Context help id definitions.
 */
public interface IContextHelpIds {

	/**
	 * UI plug-in common context help id prefix.
	 */
	public final static String PREFIX = UIPlugin.getUniqueIdentifier() + "."; //$NON-NLS-1$

	/**
	 * Script Pad console.
	 */
	public final static String SCRIPT_PAD_CONSOLE = PREFIX + "ScriptPadConsole"; //$NON-NLS-1$

	/**
	 * Script Pad error: open failed
	 */
	public final static String SCRIPT_PAD_ERROR_OPEN_FILE = PREFIX + "ScriptPadErrorOpenFile"; //$NON-NLS-1$

	/**
	 * Script Pad error: save failed
	 */
	public final static String SCRIPT_PAD_ERROR_SAVE_FILE = PREFIX + "ScriptPadErrorSaveFile"; //$NON-NLS-1$

	/**
	 * Script Pad error: script play failed
	 */
	public final static String SCRIPT_PAD_ERROR_PLAY_FAILED = PREFIX + "ScriptPadErrorPlayFailed"; //$NON-NLS-1$

	/**
	 * Communication monitor console.
	 */
	public final static String MONITOR_CONSOLE = PREFIX + "MonitorConsole"; //$NON-NLS-1$
}
