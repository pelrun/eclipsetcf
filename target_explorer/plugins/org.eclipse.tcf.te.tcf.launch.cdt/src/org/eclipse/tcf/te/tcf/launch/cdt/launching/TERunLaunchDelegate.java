/*******************************************************************************
 * Copyright (c) 2013, 2016 MontaVista Software, LLC. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Anna Dushistova (MontaVista) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.launching;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.launching.LaunchUtils;
import org.eclipse.cdt.launch.AbstractCLaunchDelegate2;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.tcf.launch.cdt.activator.Activator;
import org.eclipse.tcf.te.tcf.launch.cdt.interfaces.IRemoteTEConfigurationConstants;
import org.eclipse.tcf.te.tcf.launch.cdt.nls.Messages;
import org.eclipse.tcf.te.tcf.launch.cdt.utils.TEHelper;
import org.eclipse.tcf.te.tcf.launch.cdt.utils.TERunProcess;

public class TERunLaunchDelegate extends AbstractCLaunchDelegate2 {

	public TERunLaunchDelegate() {
		super(false);
	}

	@SuppressWarnings("unused")
	@Override
	public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		IPath exePath = checkBinaryDetails(config);
		if (exePath != null) {
			// -1. Initialize TE
			Activator.getDefault().initializeTE();
			// 0. Get the peer from the launch configuration
			IPeer peer = TEHelper.getCurrentConnection(config).getPeer();
			// 1.1. If there are commands to run before, create a script for them
			String remoteExePath = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_PATH, ""); //$NON-NLS-1$
			String arguments = getProgramArguments(config);
			String remoteLaunchCommand = remoteExePath.replaceAll("\\r", "");  //$NON-NLS-1$ //$NON-NLS-2$
			if (arguments != null && !arguments.equals("")) { //$NON-NLS-1$
				remoteLaunchCommand += " " + arguments.replaceAll("\\r", " ").replaceAll("\\n", " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			}
			IPath remotePrerunScriptPath = null;
			boolean launchAsRemoteUser = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_LAUNCH_REMOTE_USER, false);
			String userId = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_REMOTE_USER_ID, (String)null);
			String prerunCommands = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_PRERUN_COMMANDS, (String)null);
			if ( (prerunCommands != null && prerunCommands.trim().length() > 0) ||
							(launchAsRemoteUser && userId != null && userId.trim().length() > 0) ) {
				if (prerunCommands == null) { prerunCommands = ""; } //$NON-NLS-1$
				SimpleDateFormat formatter = new SimpleDateFormat ("HH-mm-ss-S", Locale.US); //$NON-NLS-1$
		        String prerunScriptNamePreffix = formatter.format( Long.valueOf(Calendar.getInstance().getTime().getTime()) );
				String prerunScriptName = prerunScriptNamePreffix + "_" + exePath.toFile().getName() + ".sh"; //$NON-NLS-1$ //$NON-NLS-2$

				IPath localTempLocation = Activator.getDefault().getStateLocation().append("prerun_commands_scripts"); //$NON-NLS-1$
				if (!localTempLocation.toFile().exists()) localTempLocation.toFile().mkdirs();
				IPath prerunScriptLocation = localTempLocation.append(prerunScriptName);
				if (prerunScriptLocation.toFile().exists()) prerunScriptLocation.toFile().delete();

				BufferedWriter writer = null;
				try {
					writer = new BufferedWriter(new FileWriter(prerunScriptLocation.toFile()));
					writer.write(NLS.bind(TEHelper.getPrerunTemplateContent(peer), prerunCommands.replaceAll("\\r", ""), remoteLaunchCommand)); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (Exception e) {
					abort(NLS.bind(Messages.TEGdbAbstractLaunchDelegate_prerunScriptCreationFailed, prerunScriptLocation.toString(), e.getLocalizedMessage()), e, ICDTLaunchConfigurationConstants.ERR_PROGRAM_NOT_EXIST);
				} finally {
					if (writer != null) {
						try {
							writer.close();
						} catch (IOException e) { /* Ignored on purpose. */ }
					}
				}
				// Grant execution permission
				prerunScriptLocation.toFile().setExecutable(true, false);

				// Download the script to the target
				remotePrerunScriptPath = new Path("/tmp").append(prerunScriptName); //$NON-NLS-1$
				try {
					TEHelper.remoteFileTransfer(peer, prerunScriptLocation.toString(), remotePrerunScriptPath.toString(), new SubProgressMonitor(monitor, 80));
				} catch (IOException e) {
					abort(NLS.bind(Messages.TEGdbAbstractLaunchDelegate_prerunScriptTransferFailed, remotePrerunScriptPath.toString(), e.getLocalizedMessage()), e, ICDTLaunchConfigurationConstants.ERR_PROGRAM_NOT_EXIST);
				} finally {
					// Remove local temporal script
					prerunScriptLocation.toFile().delete();
				}
			}

			// 1.2. Download binary if needed
			monitor.setTaskName(Messages.TEGdbAbstractLaunchDelegate_downloading);
			boolean skipDownload = config.getAttribute(IRemoteTEConfigurationConstants.ATTR_SKIP_DOWNLOAD_TO_TARGET, false);

			if (!skipDownload) {
				try {
					TEHelper.remoteFileTransfer(peer, exePath.toString(), remoteExePath, new SubProgressMonitor(monitor, 80));
				} catch (IOException e) {
					abort(NLS.bind(Messages.TEGdbAbstractLaunchDelegate_filetransferFailed, e.getLocalizedMessage()), e, ICDTLaunchConfigurationConstants.ERR_PROGRAM_NOT_EXIST);
				}
			}

			// 2. Run the binary
			monitor.setTaskName(Messages.TEGdbAbstractLaunchDelegate_starting_debugger);

			Map<String,String> env = config.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, (Map<String,String>)null);
			if (remotePrerunScriptPath != null) {
				// Pass the user id as an argument to the script
				String launchArguments = ""; //$NON-NLS-1$
				if (launchAsRemoteUser && userId != null && userId.trim().length() > 0) {
					launchArguments = "-u__ " + userId;  //$NON-NLS-1$
				}
				new TERunProcess(launch, remotePrerunScriptPath.toString(), launchArguments, env, renderProcessLabel(exePath.toString()), peer, new SubProgressMonitor(monitor, 20));
			} else {
				new TERunProcess(launch, remoteExePath, arguments, env, renderProcessLabel(exePath.toString()), peer, new SubProgressMonitor(monitor, 20));
			}
		}
	}

	protected IPath checkBinaryDetails(final ILaunchConfiguration config) throws CoreException {
		// First verify we are dealing with a proper project.
		ICProject project = verifyCProject(config);
		// Now verify we know the program to debug.
		IPath exePath = LaunchUtils.verifyProgramPath(config, project);
		// Finally, make sure the program is a proper binary.
		LaunchUtils.verifyBinary(config, exePath);
		return exePath;
	}

	protected String getProgramArguments(ILaunchConfiguration config) throws CoreException {
		return org.eclipse.cdt.launch.LaunchUtils.getProgramArguments(config);
	}

	protected String renderProcessLabel(String commandLine) {
		String format = "{0} ({1})"; //$NON-NLS-1$
		String timestamp = DateFormat.getInstance().format(new Date(System.currentTimeMillis()));
		return MessageFormat.format(format, new Object[]{commandLine, timestamp});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.launch.AbstractCLaunchDelegate2#getPluginID()
	 */
	@Override
	protected String getPluginID() {
		return Activator.getUniqueIdentifier();
	}

}
