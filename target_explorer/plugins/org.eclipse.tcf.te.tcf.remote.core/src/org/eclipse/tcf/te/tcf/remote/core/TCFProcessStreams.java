/*******************************************************************************
 * Copyright (c) 2014, 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.remote.core;

import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.launcher.IProcessStreamsProxy;

public class TCFProcessStreams implements IProcessStreamsProxy {

	private InputStream fRemoteStderr;
	private OutputStream fRemoteStdin;
	private InputStream fRemoteStdout;

	@Override
	public void connectOutputStreamMonitor(InputStream stream) {
		fRemoteStdout = stream;
	}

	@Override
	public void connectInputStreamMonitor(OutputStream stream) {
		fRemoteStdin = stream;
	}

	@Override
	public void connectErrorStreamMonitor(InputStream stream) {
		fRemoteStderr = stream;
	}

	@Override
	public void dispose(ICallback callback) {
		if (callback != null)
			callback.done(this, Status.OK_STATUS);
	}

	public InputStream getStdout() {
	    return fRemoteStdout;
    }

	public InputStream getStderr() {
	    return fRemoteStderr;
    }

	public OutputStream getStdin() {
	    return fRemoteStdin;
    }
}
