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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.EventObject;

import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.IRemoteProcess;
import org.eclipse.remote.core.IRemoteProcessBuilder;
import org.eclipse.remote.core.IRemoteProcessControlService;
import org.eclipse.tcf.services.IProcesses.ProcessContext;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.tcf.processes.core.launcher.ProcessLauncher;
import org.eclipse.tcf.te.tcf.processes.core.launcher.ProcessStateChangeEvent;

public class TCFProcess implements IRemoteProcess, IRemoteProcessControlService, IEventListener {
	private InputStream fStdout;
	private InputStream fStderr;
	private OutputStream fStdin;

	private PipedOutputStream fCombinedOutput;
	private int fReadersDone;

	private final ProcessLauncher fLauncher;
	private int fExitValue;
	private boolean fCompleted;
	private IRemoteProcessBuilder fBuilder;

	private class StreamForwarder implements Runnable {
		private final static int BUF_SIZE = 8192;

		private final InputStream fInput;
		private final OutputStream fOutput;

		public StreamForwarder(InputStream input, OutputStream output) {
			fInput = input;
			fOutput = output;
		}

		@Override
        public void run() {
			int len;
			byte b[] = new byte[BUF_SIZE];
			try {
				while ((len = fInput.read(b)) > 0) {
					fOutput.write(b, 0, len);
				}
			} catch (IOException e) {
			}
			onProcReaderDone();
		}
	}

	private static class NullInputStream extends InputStream {
        public NullInputStream() {
        }

		@Override
		public int read() throws IOException {
			return -1;
		}

		@Override
		public int available() {
			return 0;
		}
	}

	public TCFProcess(TCFProcessBuilder builder, ProcessLauncher launcher) {
		fBuilder = builder;
		fLauncher = launcher;
		EventManager.getInstance().addEventListener(this, ProcessStateChangeEvent.class);
	}

	@Override
	public IRemoteProcessBuilder getProcessBuilder() {
		return fBuilder;
	}

	@Override
	public IRemoteConnection getRemoteConnection() {
		return fBuilder.getRemoteConnection();
	}

	@Override
	public IRemoteProcess getRemoteProcess() {
		return this;
	}

	@Override
	public <T extends Service> T getService(Class<T> service) {
		if (service.isAssignableFrom(TCFProcess.class))
			return service.cast(this);
		return null;
	}

	@Override
	public <T extends Service> boolean hasService(Class<T> service) {
		return service.isAssignableFrom(TCFProcess.class);
	}

	public void connectStreams(TCFProcessStreams streams, boolean redirectStderr) throws IOException {
		if (redirectStderr) {
			fCombinedOutput = new PipedOutputStream();
			fStdout = new PipedInputStream(fCombinedOutput);
			fStderr = null;

			new Thread(new StreamForwarder(streams.getStdout(), fCombinedOutput)).start();
			new Thread(new StreamForwarder(streams.getStderr(), fCombinedOutput)).start();
		} else {
			fStdout = streams.getStdout();
			fStderr = streams.getStderr();
		}
		fStdin = streams.getStdin();
	}

	protected void onProcReaderDone() {
		synchronized (this) {
			if (++fReadersDone == 2) {
				try {
	                fCombinedOutput.close();
                } catch (IOException e) {
                }
			}
		}
    }

	@Override
	public void destroy() {
		synchronized(this) {
			if (fCompleted)
				return;
		}

		fLauncher.cancel();
		fLauncher.terminate();
		synchronized (this) {
			if (!fCompleted) {
				fExitValue = -1;
				fCompleted = true;
				notifyAll();
			}
		}
		EventManager.getInstance().removeEventListener(this);
	}

	@Override
	public int exitValue() {
		return fExitValue;
	}

	@Override
	public InputStream getErrorStream() {
		return fStderr != null ? fStderr : new NullInputStream();
	}

	@Override
	public InputStream getInputStream() {
		return fStdout != null ? fStdout : new NullInputStream();
	}

	@Override
	public OutputStream getOutputStream() {
		return fStdin;
	}

	@Override
	public int waitFor() throws InterruptedException {
		synchronized (this) {
			while (!isCompleted()) {
				wait();
			}
		}
		return exitValue();
	}

	@Override
	public boolean isCompleted() {
		return fCompleted;
	}

	@Override
    public void eventFired(EventObject event) {
		if (event instanceof ProcessStateChangeEvent) {
			ProcessStateChangeEvent pscEvent = (ProcessStateChangeEvent) event;
			if (pscEvent.getEventId().equals(ProcessStateChangeEvent.EVENT_PROCESS_TERMINATED)) {
				Object source = pscEvent.getSource();
				if ((source instanceof ProcessContext)) {
					ProcessContext context = (ProcessContext) fLauncher.getAdapter(ProcessContext.class);
					if (context != null && ((ProcessContext) source).getID().equals(context.getID())) {
						synchronized (this) {
							fExitValue = pscEvent.getExitCode();
							fCompleted = true;
							notifyAll();
						}
					}
				}
			}
		}
	}


}
