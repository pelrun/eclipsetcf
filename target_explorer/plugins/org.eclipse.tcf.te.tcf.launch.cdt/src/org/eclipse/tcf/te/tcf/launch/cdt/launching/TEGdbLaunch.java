/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.launching;

import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.tcf.te.tcf.processes.core.launcher.ProcessLauncher;

/**
 * GDB launch implementation handling also the gdbserver life cycle.
 */
public class TEGdbLaunch extends GdbLaunch {
	// The process launcher used to launch the gdbserver process
	private ProcessLauncher launcher = null;

	/**
	 * Constructor
	 *
	 * @param launchConfiguration The launch configuration. Must not be <code>null</code>.
	 * @param mode The launch mode. Must not be <code>null</code>.
	 * @param locator The source locator to use for this debug session, or <code>null</code> if not supported
	 */
	public TEGdbLaunch(ILaunchConfiguration launchConfiguration, String mode, ISourceLocator locator) {
		super(launchConfiguration, mode, locator);
	}

	/**
	 * Sets the process launcher.
	 *
	 * @param launcher The process launcher or <code>null</code>.
	 */
	public final void setLauncher(ProcessLauncher launcher) {
		this.launcher = launcher;
	}

	/**
	 * Returns the process launcher.
	 *
	 * @return The process launcher or <code>null</code>.
	 */
	public final ProcessLauncher getLauncher() {
		return launcher;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.dsf.gdb.launching.GdbLaunch#shutdownSession(org.eclipse.cdt.dsf.concurrent.RequestMonitor)
	 */
	@Override
	public void shutdownSession(RequestMonitor rm) {
		RequestMonitor r = new RequestMonitor(getDsfExecutor(), rm) {
			@Override
			protected void handleCompleted() {
				ProcessLauncher launcher = getLauncher();
				if (launcher != null) launcher.terminate();
			    super.handleCompleted();
			}
		};
	    super.shutdownSession(r);
	}
}
