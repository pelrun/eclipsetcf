/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.processes.launcher;

import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.processes.ProcessOutputReaderThread;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.launcher.IProcessStreamsProxy;

/**
 * Process streams proxy implementation.
 */
public class ProcessStreamsProxy implements IProcessStreamsProxy {
	private InputStream remoteStdout;
	private InputStream remoteStderr;

	private ProcessOutputReaderThread reader;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.core.interfaces.launcher.IProcessStreamsProxy#connectOutputStreamMonitor(java.io.InputStream)
	 */
	@Override
	public void connectOutputStreamMonitor(InputStream stream) {
		// Remember the stream only. Wait for connectErrorStreamMonitor(...)
		// to attach the reader.
		remoteStdout = stream;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.core.interfaces.launcher.IProcessStreamsProxy#connectInputStreamMonitor(java.io.OutputStream)
	 */
	@Override
	public void connectInputStreamMonitor(OutputStream stream) {
		// Ignore -> nothing to send to the remote process
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.core.interfaces.launcher.IProcessStreamsProxy#connectErrorStreamMonitor(java.io.InputStream)
	 */
	@Override
	public void connectErrorStreamMonitor(InputStream stream) {
		remoteStderr = stream;

		reader = new ProcessOutputReaderThread(ProcessStreamsProxy.class.getSimpleName(), new InputStream[] { remoteStdout, remoteStderr });
		reader.start();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.core.interfaces.launcher.IProcessStreamsProxy#dispose(org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void dispose(ICallback callback) {
		// The reader closes with the stream -> nothing to do here

		// If a callback is passed in, it needs to be invoked in any case
		if (callback != null) callback.done(this, Status.OK_STATUS);
	}

	/**
	 * Returns the process output reader.
	 *
	 * @return The process output reader or <code>null</code>.
	 */
	public ProcessOutputReaderThread getOutputReader() {
		return reader;
	}

}
