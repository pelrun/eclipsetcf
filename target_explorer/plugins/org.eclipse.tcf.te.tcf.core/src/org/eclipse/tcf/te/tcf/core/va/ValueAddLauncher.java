/*******************************************************************************
 * Copyright (c) 2012 - 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.va;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.runtime.processes.ProcessLauncher;
import org.eclipse.tcf.te.runtime.processes.ProcessOutputReaderThread;
import org.eclipse.tcf.te.runtime.utils.Env;
import org.eclipse.tcf.te.runtime.utils.Host;
import org.eclipse.tcf.te.tcf.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.core.interfaces.tracing.ITraceIds;
import org.eclipse.tcf.te.tcf.core.nls.Messages;
import org.osgi.framework.Bundle;

/**
 * Value-add launcher implementation.
 */
public class ValueAddLauncher extends ProcessLauncher {
	// The target peer id
	private final String id;
	// The path of the value-add to launch
	private final IPath path;
	// The value-add id
	private final String valueAddId;
	// The process handle
	private Process process;
	// The process output reader
	private ProcessOutputReaderThread outputReader;
	// The process error reader
	private ProcessOutputReaderThread errorReader;

	/**
	 * Constructor.
	 *
	 * @param id The target peer id. Must not be <code>null</code>.
	 * @param path The value-add path. Must not be <code>null</code>.
	 * @param valueAddId The value-add id. Must not be <code>null</code>.
	 */
	public ValueAddLauncher(String id, IPath path, String valueAddId) {
		super(null, null, 0);

		Assert.isNotNull(id);
		this.id = id;
		Assert.isNotNull(path);
		this.path = path;
		Assert.isNotNull(valueAddId);
		this.valueAddId = valueAddId;
	}

	/**
	 * Returns the target peer id.
	 *
	 * @return The target peer id.
	 */
	protected final String getPeerId() {
		return id;
	}

	/**
	 * Returns the process handle.
	 *
	 * @return The process handle or <code>null</code>.
	 */
	public final Process getProcess() {
		return process;
	}

	/**
	 * Returns the process output reader.
	 *
	 * @return The process output reader or <code>null</code>.
	 */
	public final ProcessOutputReaderThread getOutputReader() {
		return outputReader;
	}

	/**
	 * Returns the process error reader.
	 *
	 * @return The process error reader or <code>null</code>.
	 */
	public final ProcessOutputReaderThread getErrorReader() {
		return errorReader;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.processes.ProcessLauncher#launch()
	 */
	@Override
	public void launch() throws ValueAddException {
		IPath dir = path.removeLastSegments(1);
		String cmd = Host.isWindowsHost() ? path.toOSString() : "./" + path.lastSegment(); //$NON-NLS-1$

		// Build up the command
		List<String> command = new ArrayList<String>();
		command.add(cmd);

		// Add command line parameters for the value-add
		addCommandLineParameters(command);
		// Add the logging command line parameters for the value-add
		addLoggingCommandLineParameters(command);

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_CHANNEL_MANAGER)) {
			CoreBundleActivator.getTraceHandler().trace(NLS.bind(Messages.ValueAddLauncher_launch_command, new Object[] { command, id, valueAddId }),
														0, ITraceIds.TRACE_CHANNEL_MANAGER, IStatus.INFO, this);
		}

		// Determine the environment
		String[] envp = null;

		// Get the set of environment variables the launcher requires to set
		String[] additional = getEnvironmentVariables();
		if (additional != null && additional.length > 0) {
			// Get the native environment and merge the additional variables
			envp = Env.getEnvironment(additional, false);
		}

		// Launch the value-add
		process = exec(command.toArray(new String[command.size()]), envp, dir.toFile());

		// Launch the process output reader
		outputReader = createProcessOutputReaderThread(path, process.getInputStream());
		outputReader.start();

		// Launch the process error reader
		errorReader = createProcessOutputReaderThread(path, process.getErrorStream());
		errorReader.start();
	}

	/**
	 * Adds the given argument to the given command.
	 * <p>
	 * Custom value add launcher implementations may overwrite this method to
	 * validate and/or modify the command used to launch the value-add.
	 *
	 * @param command The command. Must not be <code>null</code>.
	 * @param arg The argument. Must not be <code>null</code>.
	 */
	protected void addToCommand(List<String> command, String arg) {
		Assert.isNotNull(command);
		Assert.isNotNull(arg);
		command.add(arg);
	}

	/**
	 * Add the value-add command line parameters to the command.
	 *
	 * @param command The command. Must not be <code>null</code>.
	 * @throws ValueAddException In case something failed while adding the command line parameters.
	 */
	protected void addCommandLineParameters(List<String> command) throws ValueAddException {
		Assert.isNotNull(command);

		// Determine a free port to use by the value-add. We must
		// avoid to launch the value-add at the default port 1534.
		int port = getFreePort();

		addToCommand(command, "-I180"); //$NON-NLS-1$
		addToCommand(command, "-S"); //$NON-NLS-1$
		addToCommand(command, "-sTCP::" + (port != -1 ? Integer.valueOf(port) : "") + ";ValueAdd=1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Add the value-add logging command line parameters to the command.
	 *
	 * @param command The command. Must not be <code>null</code>.
	 * @throws ValueAddException In case something failed while adding the logging command line parameters.
	 */
	protected void addLoggingCommandLineParameters(List<String> command) throws ValueAddException {
		Assert.isNotNull(command);

		// Enable logging?
		if (Boolean.getBoolean("va.logging.enable")) { //$NON-NLS-1$
			// Calculate the location and name of the log file
			Bundle bundle = Platform.getBundle("org.eclipse.tcf.te.tcf.log.core"); //$NON-NLS-1$
			IPath location = bundle != null ? Platform.getStateLocation(bundle) : null;
			if (location != null) {
				location = location.append(".logs"); //$NON-NLS-1$

				String name = "Output_" + valueAddId + "_" + id + ".log"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				name = name.replaceAll("\\s", "_"); //$NON-NLS-1$ //$NON-NLS-2$
				name = name.replaceAll("[:/\\;,]", "_"); //$NON-NLS-1$ //$NON-NLS-2$

				location = location.append(name);
				addToCommand(command, "-L" + location.toString()); //$NON-NLS-1$

				String level = System.getProperty("va.logging.level"); //$NON-NLS-1$
				if (level != null && !"".equals(level.trim())) { //$NON-NLS-1$
					addToCommand(command, "-l" + level.trim()); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Execute the value-add launch command.
	 *
	 * @param cmdarray Array containing the command to call and its arguments. Must not be <code>null</code>.
	 * @param envp Array of strings, each element of which has environment variable settings in the format <code>name=value</code>,
	 *             or <code>null</code> if the subprocess should inherit the environment of the current process.
	 * @param dir The working directory of the subprocess, or <code>null</code> if the subprocess should inherit
	 *            the working directory of the current process.
	 *
	 * @return The process instance.
	 * @see Runtime#exec(String[], String[], File)
	 */
	protected Process exec(String[] cmdarray, String[] envp, File dir) throws ValueAddException {
		Assert.isNotNull(cmdarray);

		Process process = null;
		try {
			process = Runtime.getRuntime().exec(cmdarray, envp, dir);
		} catch (IOException e) {
			throw new ValueAddException(e);
		}

		return process;
	}

	/**
	 * Creates the process output reader thread for the given stream.
	 *
	 * @param path The process path. Must not be <code>null</code>.
	 * @param stream The stream. Must not be <code>null</code>.
	 *
	 * @return The not yet started process output reader thread instance.
	 */
	protected ProcessOutputReaderThread createProcessOutputReaderThread(IPath path, InputStream stream) {
		Assert.isNotNull(path);
		Assert.isNotNull(stream);
		return new ProcessOutputReaderThread(path.lastSegment(), new InputStream[] { stream });
	}

	/**
	 * Determine a free port to use.
	 *
	 * @return A free port or <code>-1</code>.
	 */
	protected int getFreePort() {
		int port = -1;

		try {
			ServerSocket socket = new ServerSocket(0);
			port = socket.getLocalPort();
			socket.close();
		} catch (IOException e) { /* ignored on purpose */ }

		return port;
	}

	/**
	 * Returns the set of environment variable which needs to be added to
	 * the native environment or overwritten in the native environment in
	 * order to launch the value add successfully.
	 *
	 * @return The set of environment variables or <code>null</code>.
	 */
	protected String[] getEnvironmentVariables() {
		return null;
	}
}
