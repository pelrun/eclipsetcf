/*******************************************************************************
 * Copyright (c) 2012, 2016 Mentor Graphics Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Anna Dushistova (Mentor Graphics) - initial API and implementation
 * Anna Dushistova (Mentor Graphics) - moved to org.eclipse.cdt.launch.remote.tabs
 * Anna Dushistova (MontaVista)      - adapted from TEDSFDebuggerTab
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.launch.cdt.tabs;

import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.service.SessionType;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

public class TEApplicationDebuggerTab extends TEAbstractDebuggerTab {

	public TEApplicationDebuggerTab() {
		super(SessionType.REMOTE, false);
	}

	@Override
	public String getId() {
		return "org.eclipse.tcf.te.remotecdt.debug.debuggerTab"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.launch.cdt.tabs.TEAbstractDebuggerTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		/* If a Launch is initially created as Run and later its LaunchConfiguration is
		 * used as Debug, the class GdbDebuggerPage overwrites some debugger values
		 * which were already set during the "Run Launch Initialization".
		 * These values are backed up here and restored after setting the default ones. */
		String previousGdbPath = null;
		Boolean previousDebugOnFork = null;
		Boolean previousReverse = null;
		Boolean previousNonStop = null;
		try {
			previousGdbPath = config.getAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME, (String)null);
			if (config.hasAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_DEBUG_ON_FORK)) {
				previousDebugOnFork = Boolean.valueOf(config.getAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_DEBUG_ON_FORK, false));
			}
			if (config.hasAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_REVERSE)) {
				previousReverse = Boolean.valueOf(config.getAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_REVERSE, false));
			}
			if (config.hasAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_NON_STOP)) {
				previousNonStop = Boolean.valueOf(config.getAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_NON_STOP, false));
			}
		}
		catch (CoreException e) {
		}

		// Initialize configuration
		super.setDefaults(config);

		// Restore values previously set
		if (previousGdbPath != null) {
			config.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME, previousGdbPath);
		}
		if (previousDebugOnFork != null) {
			config.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_DEBUG_ON_FORK, previousDebugOnFork.booleanValue());
		}
		if (previousReverse != null) {
			config.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_REVERSE, previousReverse.booleanValue());
		}
		if (previousNonStop != null) {
			config.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_NON_STOP, previousNonStop.booleanValue());
		}
	}
}
