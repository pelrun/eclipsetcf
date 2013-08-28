/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.va;

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
	 * Returns the process handle.
	 *
	 * @return The process handle or <code>null</code>.
	 */
	public Process getProcess() {
		return process;
	}

	/**
	 * Returns the process output reader.
	 *
	 * @return The process output reader or <code>null</code>.
	 */
	public ProcessOutputReaderThread getOutputReader() {
		return outputReader;
	}

	/**
	 * Returns the process error reader.
	 *
	 * @return The process error reader or <code>null</code>.
	 */
	public ProcessOutputReaderThread getErrorReader() {
		return errorReader;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.processes.ProcessLauncher#launch()
	 */
	@Override
	public void launch() throws Throwable {
		IPath dir = path.removeLastSegments(1);
		String cmd = Host.isWindowsHost() ? path.toOSString() : "./" + path.lastSegment(); //$NON-NLS-1$

		// Determine a free port to use by the value-add. We must
		// avoid to launch the value-add at the default port 1534.
		int port = getFreePort();

		// Build up the command
		List<String> command = new ArrayList<String>();
		command.add(cmd);
		addToCommand(command, "-I180"); //$NON-NLS-1$
		addToCommand(command, "-S"); //$NON-NLS-1$
		addToCommand(command, "-sTCP::" + (port != -1 ? Integer.valueOf(port) : "") + ";ValueAdd=1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

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
		process = Runtime.getRuntime().exec(command.toArray(new String[command.size()]), envp, dir.toFile());

		// Launch the process output reader
		outputReader = new ProcessOutputReaderThread(path.lastSegment(), new InputStream[] { process.getInputStream() });
		outputReader.start();

		// Launch the process error reader (not buffering)
		errorReader = new ProcessOutputReaderThread(path.lastSegment(), new InputStream[] { process.getErrorStream() });
		errorReader.setBuffering(false);
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
