/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.processes;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;

/**
 * A simple process waiter class. The process waiter keeps "running" till the observed process
 * terminates or the process waiter is interrupted from external.
 */
public class ProcessWaiter extends Thread {
	// Reference to the process handle
	private Process process;
	// Reference to the callback to invoke
	private ICallback callback;
	// Flag set once the process finished
	private boolean finished;
	// The exit code of the process
	private int exitCode;

	/**
	 * Constructor.
	 *
	 * @param process The process to monitor. Must not be <code>null</code>.
	 */
	public ProcessWaiter(final Process process) {
		this(process, null);
	}

	/**
	 * Constructor.
	 *
	 * @param process The process to monitor. Must not be <code>null</code>.
	 * @param callback The callback to invoke once the process exit, or <code>null</code>.
	 */
	public ProcessWaiter(final Process process, final ICallback callback) {
		super();

		Assert.isNotNull(process);
		this.process = process;
		this.callback = callback;
		this.finished = false;
		this.exitCode = -1;
	}

	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		try {
			exitCode = process.waitFor();
		}
		catch (InterruptedException e) {
			/* ignored on purpose */
		}
		finished = true;
		
		// If there is a callback given, invoke the callback
		// with the exit code set as result
		if (callback != null) {
			callback.setResult(Integer.valueOf(exitCode));
			callback.done(this, Status.OK_STATUS);
		}
	}

	/**
	 * Returns if or if not the monitored process finished yet.
	 *
	 * @return <code>true</code> if the process finished yet, <code>false</code> otherwise
	 */
	public final boolean isFinished() {
		return finished;
	}

	/**
	 * Returns the process exit code the waiter had been monitored.
	 *
	 * @return The process exit code or <code>-1</code>.
	 */
	public final int getExitCode() {
		return exitCode;
	}
}
