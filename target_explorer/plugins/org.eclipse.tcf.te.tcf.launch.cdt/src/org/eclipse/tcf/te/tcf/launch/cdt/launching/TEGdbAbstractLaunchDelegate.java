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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DsfExecutor;
import org.eclipse.cdt.dsf.concurrent.DsfRunnable;
import org.eclipse.cdt.dsf.concurrent.ImmediateRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.Query;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate;
import org.eclipse.cdt.dsf.gdb.service.IGDBProcesses;
import org.eclipse.cdt.dsf.gdb.service.command.IGDBControl;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.tcf.core.streams.StreamsDataReceiver;
import org.eclipse.tcf.te.tcf.launch.cdt.activator.Activator;
import org.eclipse.tcf.te.tcf.launch.cdt.interfaces.IGdbserverLaunchHandlerDelegate;
import org.eclipse.tcf.te.tcf.launch.cdt.interfaces.IRemoteTEConfigurationConstants;
import org.eclipse.tcf.te.tcf.launch.cdt.nls.Messages;
import org.eclipse.tcf.te.tcf.launch.cdt.preferences.IPreferenceKeys;
import org.eclipse.tcf.te.tcf.launch.cdt.utils.TEHelper;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
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
	 * @see org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate#createGdbLaunch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.model.ISourceLocator)
	 */
	@Override
	protected GdbLaunch createGdbLaunch(ILaunchConfiguration configuration, String mode, ISourceLocator locator) throws CoreException {
	    return new TEGdbLaunch(configuration, mode, locator);
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

		// Determine if the launch is an attach launch
		final boolean isAttachLaunch = ICDTLaunchConfigurationConstants.ID_LAUNCH_C_ATTACH.equals(config.getType().getIdentifier());

		// Get the executable path (run/debug application) or the PID (attach to application)
		IPath exePath = checkBinaryDetails(config);
		String remoteExePath = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_PATH, (String)null);
		String remotePID = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_PID, (String)null);

		// Not an attach launch and the executable is not specified --> abort
		if (!isAttachLaunch && exePath == null) {
			abort(Messages.TEGdbAbstractLaunchDelegate_no_program, null, ICDTLaunchConfigurationConstants.ERR_PROGRAM_NOT_EXIST);
		}
		// Not an attach launch and the remote executable is not specified --> abort
		if (!isAttachLaunch && remoteExePath == null) {
			abort(Messages.TEGdbAbstractLaunchDelegate_no_remote_path, null, ICDTLaunchConfigurationConstants.ERR_PROGRAM_NOT_EXIST);
		}
		// Attach launch and the remote PID is not specified --> abort
		if (isAttachLaunch && remotePID == null) {
			abort(Messages.TEGdbAbstractLaunchDelegate_no_pid, null, ICDTLaunchConfigurationConstants.ERR_NO_PROCESSID);
		}

		// If an executable path is specified, download the binary if needed
		if (!isAttachLaunch && exePath != null && remoteExePath != null) {
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

		// Launch gdbserver on target
		final AtomicReference<String> gdbserverPortNumber = new AtomicReference<String>(config.getAttribute(IRemoteTEConfigurationConstants.ATTR_GDBSERVER_PORT, TEHelper.getStringPreferenceValue(isAttachLaunch ? IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH : IPreferenceKeys.PREF_GDBSERVER_PORT)));
		final AtomicReference<String> gdbserverPortNumberMappedTo = new AtomicReference<String>(config.getAttribute(IRemoteTEConfigurationConstants.ATTR_GDBSERVER_PORT_MAPPED_TO, TEHelper.getStringPreferenceValue(isAttachLaunch ? IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_MAPPED_TO : IPreferenceKeys.PREF_GDBSERVER_PORT_MAPPED_TO)));
		final String gdbserverCommand = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_GDBSERVER_COMMAND, TEHelper.getStringPreferenceValue(isAttachLaunch ? IPreferenceKeys.PREF_GDBSERVER_COMMAND_ATTACH : IPreferenceKeys.PREF_GDBSERVER_COMMAND));
		final List<String> gdbserverPortNumberAlternatives = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_GDBSERVER_PORT_ALTERNATIVES, TEHelper.getListPreferenceValue(isAttachLaunch ? IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_ALTERNATIVES : IPreferenceKeys.PREF_GDBSERVER_PORT_ALTERNATIVES));
		final List<String> gdbserverPortNumberMappedToAlternatives = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_GDBSERVER_PORT_MAPPED_TO_ALTERNATIVES, TEHelper.getListPreferenceValue(isAttachLaunch ? IPreferenceKeys.PREF_GDBSERVER_PORT_ATTACH_MAPPED_TO_ALTERNATIVES : IPreferenceKeys.PREF_GDBSERVER_PORT_MAPPED_TO_ALTERNATIVES));

		// Remember the originally configured port number and mapped to port number
		final String origGdbserverPortNumber = gdbserverPortNumber.get();
		final String origGdbserverPortNumberMappedTo = gdbserverPortNumberMappedTo.get();
		final AtomicReference<String> origGdbserverOutput = new AtomicReference<String>(null);

		ProcessLauncher launcher = null;

		final AtomicBoolean gdbserverLaunchRetry = new AtomicBoolean(false);
		final AtomicInteger indexAlternatives = new AtomicInteger(0);

		do {
			gdbserverLaunchRetry.set(false);

			final AtomicBoolean gdbServerStarted = new AtomicBoolean(false);
			final AtomicBoolean gdbServerReady = new AtomicBoolean(false);
			final AtomicBoolean gdbServerExited = new AtomicBoolean(false);
			final StringBuilder gdbServerOutput = new StringBuilder();
			final Object lock = new Object();

			String commandArguments = ""; //$NON-NLS-1$
			if (isAttachLaunch) {
				commandArguments = "--once --multi :" + gdbserverPortNumber.get(); //$NON-NLS-1$
				monitor.setTaskName(Messages.TEGdbAbstractLaunchDelegate_attaching_program);
			} else {
				commandArguments = ":" + gdbserverPortNumber.get() + " " + TEHelper.spaceEscapify(remoteExePath); //$NON-NLS-1$ //$NON-NLS-2$

				String arguments = getProgramArguments(config);
				String prelaunchCmd = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_PRERUN_COMMANDS, ""); //$NON-NLS-1$

				TEHelper.launchCmd(peer, prelaunchCmd, null, new SubProgressMonitor(monitor, 2), new Callback());

				if (arguments != null && !arguments.equals("")) { //$NON-NLS-1$
					commandArguments += " " + arguments; //$NON-NLS-1$
				}
				monitor.setTaskName(Messages.TEGdbAbstractLaunchDelegate_starting_program);
			}

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
						// Check if the gdbserver exited because the port is already in use
						if (gdbServerOutput.toString().contains("Address already in use.")) { //$NON-NLS-1$
							// If we have still alternatives, then retry the gdbserver launch
							// with an alternative port
							if (!gdbserverPortNumberAlternatives.isEmpty()) {
								String newPort = null;
								String newPortMappedTo = null;

								do {
									newPort = gdbserverPortNumberAlternatives.get(indexAlternatives.get());
									if (gdbserverPortNumber.get().equals(newPort)) {
										newPort = null;
									} else {
										newPortMappedTo = gdbserverPortNumberMappedToAlternatives != null && !gdbserverPortNumberMappedToAlternatives.isEmpty() ? gdbserverPortNumberMappedToAlternatives.get(indexAlternatives.get()) : null;
									}
									indexAlternatives.getAndIncrement();
								} while (newPort == null && indexAlternatives.get() < gdbserverPortNumberAlternatives.size());

								if (newPort != null) {
									// Remember the original error
									if (origGdbserverOutput.get() == null) origGdbserverOutput.set(gdbServerOutput.toString());
									// Set the flag to retry the gdbserver launch
									gdbserverLaunchRetry.set(true);
									// Update the ports
									gdbserverPortNumber.set(newPort);
									gdbserverPortNumberMappedTo.set(newPortMappedTo);
								}
							}
						}
						gdbServerExited.set(true);
						synchronized (lock) {
							lock.notifyAll();
						}
					}

				}
			};

			launcher = TEHelper.launchCmd(peer, gdbserverCommand, commandArguments, listener, new SubProgressMonitor(monitor, 3), callback);

			// Now wait until gdbserver is up and running on the remote host
			while (!gdbServerReady.get() && !gdbServerExited.get()) {
				if (monitor.isCanceled()) {
					// gdbserver launch failed
					// Need to shutdown the DSF launch session because it is
					// partially started already.
					shutdownSession(l, Messages.TEGdbAbstractLaunchDelegate_canceledMsg, launcher);
				}
				if (gdbServerStarted.get() && launcher.getChannel() == null) {
					// gdbserver died or exited. Wait a little bit to process
					// possible gdbserver output before shutting down the session.
					synchronized (lock) {
						try {
							lock.wait(500);
						}
						catch (InterruptedException e) { /* ignored on purpose */ }
					}
					if (!gdbserverLaunchRetry.get()) {
						shutdownSession(l, origGdbserverOutput.get() != null ? origGdbserverOutput.get() : gdbServerOutput.toString(), launcher);
					}
				}
				synchronized (lock) {
					try {
						lock.wait(300);
					}
					catch (InterruptedException e) { /* ignored on purpose */ }
				}
			}

			// If the gdbserver exited, also shutdown the DSF launch session
			if (!gdbserverLaunchRetry.get() && gdbServerExited.get()) {
				shutdownSession(l, origGdbserverOutput.get() != null ? origGdbserverOutput.get() : gdbServerOutput.toString(), launcher);
			}
		} while (gdbserverLaunchRetry.get());

		// Set the launcher to the launch
		if (launch instanceof TEGdbLaunch) {
			((TEGdbLaunch)launch).setLauncher(launcher);
		}

		// Let debugger know how gdbserver was started on the remote
		ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
		if (!origGdbserverPortNumber.equals(gdbserverPortNumber.get())) {
			wc.setAttribute(IRemoteTEConfigurationConstants.ATTR_GDBSERVER_PORT, gdbserverPortNumber.get());
		}
		if (origGdbserverPortNumberMappedTo != null && !origGdbserverPortNumberMappedTo.equals(gdbserverPortNumberMappedTo.get())) {
			wc.setAttribute(IRemoteTEConfigurationConstants.ATTR_GDBSERVER_PORT_MAPPED_TO, gdbserverPortNumberMappedTo.get());
		}
		wc.setAttribute(IGDBLaunchConfigurationConstants.ATTR_REMOTE_TCP, true);
		wc.setAttribute(IGDBLaunchConfigurationConstants.ATTR_HOST, TEHelper.getCurrentConnection(config).getPeer().getAttributes().get(IPeer.ATTR_IP_HOST));
		wc.setAttribute(IGDBLaunchConfigurationConstants.ATTR_PORT, gdbserverPortNumberMappedTo.get() == null || "".equals(gdbserverPortNumberMappedTo.get()) ? gdbserverPortNumber.get() : gdbserverPortNumberMappedTo.get()); //$NON-NLS-1$
		wc.doSave();
		try {
			super.launch(config, mode, launch, monitor);
		}
		catch (CoreException ex) {
			// Launch failed, need to kill gdbserver
			if (launcher != null) launcher.terminate();
			// report failure further
			throw ex;
		}
		finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate#launchDebugSession(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
    @Override
	protected void launchDebugSession(ILaunchConfiguration config, ILaunch l, IProgressMonitor monitor) throws CoreException {
	    super.launchDebugSession(config, l, monitor);

		// Determine if the launch is an attach launch
		final boolean isAttachLaunch = ICDTLaunchConfigurationConstants.ID_LAUNCH_C_ATTACH.equals(config.getType().getIdentifier());
		if (!isAttachLaunch)
			return;

        boolean ok = false;
		try {
			if (!(l instanceof GdbLaunch))
				throw new DebugException(new Status(IStatus.ERROR, Activator.getUniqueIdentifier(), "Unexpected launch: " + l.getClass().getName())); //$NON-NLS-1$

			final IPath exePath = checkBinaryDetails(config);
			if (exePath == null)
				throw new DebugException(new Status(IStatus.ERROR, Activator.getUniqueIdentifier(), "No executable specified")); //$NON-NLS-1$

			final GdbLaunch launch = (GdbLaunch) l;
			final DsfExecutor executor = launch.getDsfExecutor();
			final String pid = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_PID, "0"); //$NON-NLS-1$
			Query<IDMContext> query = new Query<IDMContext>() {
				@Override
                protected void execute(DataRequestMonitor<IDMContext> rm) {
					DsfServicesTracker tracker = new DsfServicesTracker(Activator.getDefault().getBundle().getBundleContext(), launch.getSession().getId());
					try {
						IGDBProcesses gdbProcesses = tracker.getService(IGDBProcesses.class);
						IGDBControl commandControl = tracker.getService(IGDBControl.class);
						gdbProcesses.attachDebuggerToProcess(
										gdbProcesses.createProcessContext(commandControl.getContext(), pid),
										exePath.toString(),
										rm);

					} finally {
						tracker.dispose();
					}
				}
			};
			executor.execute(query);
			if (query.get() != null)
				ok = true;
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
			throw new DebugException(new Status(IStatus.ERROR, Activator.getUniqueIdentifier(), "Cannot attach to process", e)); //$NON-NLS-1$
        } finally {
			if (!ok) {
                cleanupLaunch();
            }
        }
	}

	/**
	 * Shutdown the GDB debug session.
	 *
	 * @param launch The GDB launch. Must not be <code>null</code>.
	 * @param details Error message, may be <code>null</code>
	 * @throws CoreException If the GDB debug session shutdown failed.
	 */
	protected void shutdownSession(final GdbLaunch launch, final String details, final ProcessLauncher launcher) throws CoreException {
		Assert.isNotNull(launch);
		try {
			launch.getSession().getExecutor().submit(new DsfRunnable() {
				@Override
				public void run() {
					// Avoid an NPE while running the shutdown
					if (launch.getDsfExecutor() != null) {
						launch.shutdownSession(new ImmediateRequestMonitor());
					}
					// Make sure that the gdbserver is killed and the launcher resources gets disposed
					if (launcher != null) launcher.terminate();
				}
			}).get(1000, TimeUnit.MILLISECONDS);
		}
		catch (RejectedExecutionException e) {
			// Session disposed.
		}
		catch (Exception e) {
			// Ignore exceptions during shutdown.
		}

		// Normalize the gdbserver start failure details
		String details2 = normalizeDetails(launch, details);

		String msg = Messages.TEGdbAbstractLaunchDelegate_gdbserverFailedToStartErrorMessage;
		if (details2 != null && details2.length() > 0) msg = NLS.bind(Messages.TEGdbAbstractLaunchDelegate_gdbserverFailedToStartErrorWithDetails, details2);
		abort(msg, null, ICDTLaunchConfigurationConstants.ERR_DEBUGGER_NOT_INSTALLED);
	}

	/**
	 * Normalize the gdbserver launch failure details message.
	 *
	 * @param launch The launch. Must not be <code>null</code>
	 * @param details The details message or <code>null</code>.
	 *
	 * @return The normalized details message or <code>null</code>.
	 *
	 * @throws CoreException In case of an failure accessing any launch configuration attribute or similar.
	 */
	protected String normalizeDetails(final GdbLaunch launch, final String details) throws CoreException {
		Assert.isNotNull(launch);

		// Get the launch configuration from the launch
		final ILaunchConfiguration lc = launch.getLaunchConfiguration();

		String d = details;
		if (d != null && !"".equals(d)) { //$NON-NLS-1$
			// Try the delegate first if available
			IPeerNode peerNode = TEHelper.getCurrentConnection(lc);
			Assert.isNotNull(peerNode);
			IGdbserverLaunchHandlerDelegate delegate = ServiceUtils.getDelegateServiceDelegate(peerNode, peerNode, IGdbserverLaunchHandlerDelegate.class);
			if (delegate != null) {
				d = delegate.normalizeGdbserverLaunchFailureDetailsMessage(launch, details);
			} else {
				// Rewrite "Address in use" error.
				if (d.contains("Address already in use.")) { //$NON-NLS-1$
					// Get host and port
					String host = lc.getAttribute(IGDBLaunchConfigurationConstants.ATTR_HOST, (String)null);
					String port = lc.getAttribute(IGDBLaunchConfigurationConstants.ATTR_PORT, (String)null);

					String address = host + (port != null ? ":" + port : ""); //$NON-NLS-1$ //$NON-NLS-2$
					d = NLS.bind(Messages.TEGdbAbstractLaunchDelegate_error_addressInUse, address);
				}
			}
		}

		return d;
	}

	protected String getProgramArguments(ILaunchConfiguration config) throws CoreException {
		String args = config.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, (String) null);
		if (args != null) {
			args = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(args);
		}
		return args;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate#getPluginID()
	 */
	@Override
	protected String getPluginID() {
		return Activator.getUniqueIdentifier();
	}

}
