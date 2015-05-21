/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.launching;

import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.concurrent.DsfRunnable;
import org.eclipse.cdt.dsf.concurrent.ImmediateRequestMonitor;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.tcf.core.streams.StreamsDataReceiver;
import org.eclipse.tcf.te.tcf.launch.cdt.activator.Activator;
import org.eclipse.tcf.te.tcf.launch.cdt.interfaces.IRemoteTEConfigurationConstants;
import org.eclipse.tcf.te.tcf.launch.cdt.nls.Messages;
import org.eclipse.tcf.te.tcf.launch.cdt.utils.TEHelper;
import org.eclipse.tcf.te.tcf.processes.core.launcher.ProcessLauncher;

/**
 * Abstract launch delegate implementation handling launching the gdbserver via TCF/TE.
 */
public abstract class TEGdbAbstractLaunchDelegate extends GdbLaunchDelegate {

	/**
	 * Constructor
	 */
	public TEGdbAbstractLaunchDelegate() {
		super();
	}

	/**
	 * Constructor
	 *
	 * @param requireCProject <code>True</code> if a C project is required for launching,
	 *            <code>false</code> otherwise.
	 */
	public TEGdbAbstractLaunchDelegate(boolean requireCProject) {
		super(requireCProject);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		// If not of the expected type --> return immediately
		if (!(launch instanceof GdbLaunch)) return;

		// Initialize TE
		Activator.getDefault().initializeTE();
		// Get the peer from the launch configuration
		IPeer peer = TEHelper.getCurrentConnection(config).getPeer();

		// Get the executable path (run/debug application) or the PID (attach to application)
		IPath exePath = checkBinaryDetails(config);
		String remoteExePath = null;
		String remotePID = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_PID, (String)null);

		// If neither executable not PID is given --> abort
		if (exePath == null && remotePID == null) {
			abort(Messages.TEGdbAbstractLaunchDelegate_no_program_or_pid, null, ICDTLaunchConfigurationConstants.ERR_PROGRAM_NOT_EXIST);
		}

		// If an executable path is specified, download the binary if needed
		if (exePath != null) {
			remoteExePath = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_PATH, ""); //$NON-NLS-1$
			monitor.setTaskName(Messages.TEGdbAbstractLaunchDelegate_downloading);
			boolean skipDownload = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_SKIP_DOWNLOAD_TO_TARGET, false);

			if (!skipDownload) {
				try {
					TEHelper.remoteFileTransfer(peer, exePath.toString(), remoteExePath, new SubProgressMonitor(monitor, 80));
				}
				catch (IOException e) {
					abort(NLS.bind(Messages.TEGdbAbstractLaunchDelegate_filetransferFailed, e.getLocalizedMessage()), e, ICDTLaunchConfigurationConstants.ERR_PROGRAM_NOT_EXIST);
				}
			}
		}

		// 2.Launch gdbserver on target
		String gdbserverPortNumber = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_GDBSERVER_PORT, IRemoteTEConfigurationConstants.ATTR_GDBSERVER_PORT_DEFAULT);
		String gdbserverPortNumberMappedTo = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_GDBSERVER_PORT_MAPPED_TO, (String) null);
		String gdbserverCommand = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_GDBSERVER_COMMAND, IRemoteTEConfigurationConstants.ATTR_GDBSERVER_COMMAND_DEFAULT);
		String commandArguments = ""; //$NON-NLS-1$
		if (remotePID != null && !"".equals(remotePID)) { //$NON-NLS-1$
			commandArguments = "--attach :" + gdbserverPortNumber + " " + remotePID; //$NON-NLS-1$ //$NON-NLS-2$
			monitor.setTaskName(Messages.TEGdbAbstractLaunchDelegate_attaching_program);
		} else if (remoteExePath != null && !"".equals(remoteExePath)) { //$NON-NLS-1$
			commandArguments = ":" + gdbserverPortNumber + " " + TEHelper.spaceEscapify(remoteExePath); //$NON-NLS-1$ //$NON-NLS-2$

			String arguments = getProgramArguments(config);
			String prelaunchCmd = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_PRERUN_COMMANDS, ""); //$NON-NLS-1$

			TEHelper.launchCmd(peer, prelaunchCmd, null, new SubProgressMonitor(monitor, 2), new Callback());

			if (arguments != null && !arguments.equals("")) { //$NON-NLS-1$
				commandArguments += " " + arguments; //$NON-NLS-1$
			}
			monitor.setTaskName(Messages.TEGdbAbstractLaunchDelegate_starting_program);
		}

		final AtomicBoolean gdbServerStarted = new AtomicBoolean(false);
		final AtomicBoolean gdbServerReady = new AtomicBoolean(false);
		final AtomicBoolean gdbServerExited = new AtomicBoolean(false);
		final StringBuffer gdbServerOutput = new StringBuffer();
		final Object lock = new Object();

		final GdbLaunch l = (GdbLaunch) launch;
		final Callback callback = new Callback() {
			@Override
			protected void internalDone(Object caller, IStatus status) {
				if (!status.isOK()) {
					gdbServerOutput.append(status.getMessage());
					gdbServerExited.set(true);
					synchronized (lock) {
						lock.notifyAll();
					}
				}
				else {
					gdbServerStarted.set(true);
				}
				super.internalDone(caller, status);
			}
		};

		StreamsDataReceiver.Listener listener = new StreamsDataReceiver.Listener() {

			@Override
			public void dataReceived(String data) {
				gdbServerOutput.append(data);
				if (data.contains("Listening on port")) { //$NON-NLS-1$
					gdbServerReady.set(true);
					synchronized (lock) {
						lock.notifyAll();
					}
				}
				else if (data.contains("GDBserver exiting") || data.contains("Exiting")) { //$NON-NLS-1$ //$NON-NLS-2$
					gdbServerExited.set(true);
					synchronized (lock) {
						lock.notifyAll();
					}
				}

			}
		};

		ProcessLauncher launcher = TEHelper.launchCmd(peer, gdbserverCommand, commandArguments, listener, new SubProgressMonitor(monitor, 3), callback);

		// Now wait until gdbserver is up and running on the remote host
		while (!gdbServerReady.get() && !gdbServerExited.get()) {
			if (monitor.isCanceled()) {
				// gdbserver launch failed
				// Need to shutdown the DSF launch session because it is
				// partially started already.
				shutdownSession(l, Messages.TEGdbAbstractLaunchDelegate_canceledMsg);
			}
			if (gdbServerStarted.get() && launcher.getChannel() == null) {
				// gdbserver died
				shutdownSession(l, gdbServerOutput.toString());
			}
			synchronized (lock) {
				try {
					lock.wait(300);
				}
				catch (InterruptedException e) {
				}
			}
		}

		// If the gdbserver exited, also shutdown the DSF launch session
		if (gdbServerExited.get()) {
			shutdownSession(l, gdbServerOutput.toString());
		}

		// 3. Let debugger know how gdbserver was started on the remote
		ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
		wc.setAttribute(IGDBLaunchConfigurationConstants.ATTR_REMOTE_TCP, true);
		wc.setAttribute(IGDBLaunchConfigurationConstants.ATTR_HOST, TEHelper.getCurrentConnection(config).getPeer().getAttributes().get(IPeer.ATTR_IP_HOST));
		wc.setAttribute(IGDBLaunchConfigurationConstants.ATTR_PORT, gdbserverPortNumberMappedTo == null || "".equals(gdbserverPortNumberMappedTo) ? gdbserverPortNumber : gdbserverPortNumberMappedTo); //$NON-NLS-1$
		wc.doSave();
		try {
			super.launch(config, mode, launch, monitor);
		}
		catch (CoreException ex) {
			// Launch failed, need to kill gdbserver
			launcher.terminate();
			// report failure further
			throw ex;
		}
		finally {
			monitor.done();
		}
	}

	/**
	 * Shutdown the GDB debug session.
	 *
	 * @param launch The GDB launch. Must not be <code>null</code>.
	 * @throws CoreException If the GDB debug session shutdown failed.
	 */
	protected void shutdownSession(final GdbLaunch launch) throws CoreException {
		shutdownSession(launch, null);
	}

	/**
	 * Shutdown the GDB debug session.
	 *
	 * @param launch The GDB launch. Must not be <code>null</code>.
	 * @param details Error message, may be <code>null</code>
	 * @throws CoreException If the GDB debug session shutdown failed.
	 */
	protected void shutdownSession(final GdbLaunch launch, String details) throws CoreException {
		Assert.isNotNull(launch);
		try {
			launch.getSession().getExecutor().submit(new DsfRunnable() {
				@Override
				public void run() {
					// Avoid an NPE while running the shutdown
					if (launch.getDsfExecutor() != null) {
						launch.shutdownSession(new ImmediateRequestMonitor());
					}
				}
			}).get(1000, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException e) {
			// Session disposed.
		}
		catch (Exception e) {
			// Ignore exceptions during shutdown.
		}

		String msg = Messages.TEGdbAbstractLaunchDelegate_gdbserverFailedToStartErrorMessage;
		if (details != null && details.length() > 0) msg = NLS.bind(Messages.TEGdbAbstractLaunchDelegate_gdbserverFailedToStartErrorWithDetails, details);
		abort(msg, null, ICDTLaunchConfigurationConstants.ERR_DEBUGGER_NOT_INSTALLED);
	}

	protected String getProgramArguments(ILaunchConfiguration config) throws CoreException {
		String args = config.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, (String) null);
		if (args != null) {
			args = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(args);
		}
		return args;
	}

	@Override
	protected String getPluginID() {
		return Activator.PLUGIN_ID;
	}

}
