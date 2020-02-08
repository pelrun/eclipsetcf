/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.launcher;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalTabListener;

/**
 * Remote process launcher terminal tab listener implementation.
 * <p>
 * <b>Note:</b> The notifications may occur in every thread!
 */
public class ProcessLauncherTerminalTabListener extends PlatformObject implements ITerminalTabListener {
	// Reference to the parent launcher
	private final ProcessLauncher parent;

	/**
	 * Constructor.
	 *
	 * @param parent The parent launcher. Must not be <code>null</code>.
	 */
	public ProcessLauncherTerminalTabListener(ProcessLauncher parent) {
		super();

		Assert.isNotNull(parent);
		this.parent = parent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.core.terminals.interfaces.ITerminalTabListener#terminalTabDisposed(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void terminalTabDisposed(Object source, Object data) {
		Assert.isNotNull(source);

		// The custom data object must be of type TcfRemoteProcessLauncher and match the parent launcher
		if (data instanceof ProcessLauncher && parent.equals(data)) {
			// Terminate the remote process (leads to the disposal of the parent)
			parent.terminate();
		}
	}
}
