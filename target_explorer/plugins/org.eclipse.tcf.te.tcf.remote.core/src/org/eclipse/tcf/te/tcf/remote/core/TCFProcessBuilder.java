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

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.remote.core.AbstractRemoteProcessBuilder;
import org.eclipse.remote.core.IRemoteProcess;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.launcher.IProcessLauncher;
import org.eclipse.tcf.te.tcf.processes.core.launcher.ProcessLauncher;
import org.eclipse.tcf.te.tcf.remote.core.operation.TCFOperationStartProcess;

public class TCFProcessBuilder extends AbstractRemoteProcessBuilder {
	private final TCFConnection fConnection;
	private Map<String, String> fEnv;

	public TCFProcessBuilder(TCFConnection connection, List<String> command) {
		super(connection.getRemoteConnection(), command);
		fConnection = connection;
	}

	public TCFProcessBuilder(TCFConnection connection, String... command) {
		this(connection, Arrays.asList(command));
	}

	@Override
	public IFileStore directory() {
		IFileStore dir = super.directory();
		if (dir == null) {
			dir = fConnection.getResource(fConnection.getWorkingDirectory());
			directory(dir);
		}
		return dir;
	}

	@Override
	public Map<String, String> environment() {
		if (fEnv == null) {
			fEnv = new HashMap<String, String>(fConnection.getEnv());
		}
		return fEnv;
	}

	@Override
	public int getSupportedFlags() {
		return 0;
	}

	@Override
	public IRemoteProcess start(int flags) throws IOException {
		if (!fConnection.isOpen()) {
			throw new IOException(format(Messages.TCFProcessBuilder_errorConnectionClosed, fConnection.getName()));
		}
		IFileStore dirStore = super.directory();
		String dir;
		if (dirStore instanceof TCFFileStore) {
			dir = ((TCFFileStore) dirStore).getPath();
		} else {
			dir = ""; //$NON-NLS-1$
		}

		List<String> cmd = command();
		if (cmd.size() < 1)
			throw new IOException(Messages.TCFProcessBuilder_errorNoCommand);

		String process= cmd.get(0);
		String[] args= cmd.subList(1, cmd.size()).toArray(new String[cmd.size()-1]);

		IPropertiesContainer launcherProps = new PropertiesContainer();
		launcherProps.setProperty(IProcessLauncher.PROP_PROCESS_CWD, dir);
		launcherProps.setProperty(IProcessLauncher.PROP_PROCESS_PATH, process);
		launcherProps.setProperty(IProcessLauncher.PROP_PROCESS_ARGS, args);
		launcherProps.setProperty(IChannelManager.FLAG_FORCE_NEW, Boolean.FALSE);
		launcherProps.setProperty(ProcessLauncher.PROCESS_LAUNCH_FAILED_MESSAGE, format("Failed to launch process {0}" , process)); //$NON-NLS-1$

		TCFProcessStreams streamsProxy = new TCFProcessStreams();
		ProcessLauncher launcher = new ProcessLauncher(streamsProxy) {
			@SuppressWarnings("synthetic-access")
            @Override
            protected void mergeEnvironment(Map<String,String> processEnv, Map<String,String> processEnvDiff) {
				if (fEnv != null) {
					processEnv.clear();
					processEnv.putAll(fEnv);
				}
			}
		};

		TCFProcess remoteProcess = new TCFProcess(this, launcher);
		boolean ok = false;
		try {
			new TCFOperationStartProcess(fConnection.getPeerNode().getPeer(),
							launcher, launcherProps).execute(SubMonitor.convert(null));
			remoteProcess.connectStreams(streamsProxy, redirectErrorStream());
			ok = true;
			return remoteProcess;
		} catch (OperationCanceledException e) {
			return null;
		} catch (CoreException e) {
			throw new IOException(Messages.TCFProcessBuilder_errorLaunchingProcess, e);
		} finally {
			if (!ok)
				remoteProcess.destroy();
		}
	}
}