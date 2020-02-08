/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * IBM Corporation              - initial API and implementation
 * Anna Dushistova (MontaVista) - adapted from org.eclipse.debug.core.model.RuntimeProcess
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.launch.cdt.utils;

import java.util.EventObject;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.internal.core.DebugCoreMessages;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.services.IProcesses.ProcessContext;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.tcf.launch.cdt.activator.Activator;
import org.eclipse.tcf.te.tcf.processes.core.launcher.ProcessLauncher;
import org.eclipse.tcf.te.tcf.processes.core.launcher.ProcessStateChangeEvent;

@SuppressWarnings("restriction")
public class TERunProcess extends PlatformObject implements IProcess,
		IEventListener {

	private ProcessLauncher prLauncher;
	private String prName;
	private boolean terminated;
	private ILaunch launch;
	private ProcessContext context;
	private int exitValue;

	public TERunProcess(ILaunch launch, String remoteExePath, String arguments,
					String label, IPeer peer, SubProgressMonitor monitor) {
		this(launch, remoteExePath, arguments, null, label, peer, monitor);
	}

	public TERunProcess(ILaunch launch, String remoteExePath, String arguments,
			Map<String, String> env, String label, IPeer peer, SubProgressMonitor monitor) {
		this.launch = launch;
		// initializeAttributes(attributes);
		prName = remoteExePath;
		terminated = false;
		launch.addProcess(this);
		EventManager.getInstance().addEventListener(this,
				ProcessStateChangeEvent.class);
		try {
			prLauncher = TEHelper.launchCmdWithEnv(peer, null, remoteExePath, arguments,
					env, null, monitor, new Callback());
		} catch (CoreException e) {
			Activator.getDefault().getLog().log(
					new Status(IStatus.ERROR, Activator.getUniqueIdentifier(),
							"Error launching remote process: "+prName, e)); //$NON-NLS-1$
			String reason = e.getMessage();
			if (reason == null)
				reason = "Unknown Reason"; //$NON-NLS-1$
			prName += " (Failed to start: " + reason + ')'; //$NON-NLS-1$
		}
		fireCreationEvent();
	}

	@Override
    public Object getAdapter(Class adapter) {
		if (adapter.equals(IProcess.class)) {
			return this;
		}
		if (adapter.equals(IDebugTarget.class)) {
			ILaunch launch = getLaunch();
			IDebugTarget[] targets = launch.getDebugTargets();
			for (int i = 0; i < targets.length; i++) {
				if (this.equals(targets[i].getProcess())) {
					return targets[i];
				}
			}
			return null;
		}
		if (adapter.equals(ILaunch.class)) {
			return getLaunch();
		}
		// CONTEXTLAUNCHING
		if (adapter.equals(ILaunchConfiguration.class)) {
			return getLaunch().getLaunchConfiguration();
		}
		return super.getAdapter(adapter);
	}

	@Override
    public boolean canTerminate() {
		return prLauncher != null && !terminated;
	}

	@Override
    public boolean isTerminated() {
		return prLauncher == null || terminated;
	}

	@Override
    public void terminate() throws DebugException {
		if (!isTerminated()) {
			prLauncher.terminate();
		}
	}

	@Override
    public String getLabel() {
		return prName;
	}

	@Override
    public ILaunch getLaunch() {
		return launch;
	}

	@Override
    public IStreamsProxy getStreamsProxy() {
		// NOT SUPPORTED
		return null;
	}

	@Override
    public void setAttribute(String key, String value) {
		// NOT SUPPORTED FOR NOW

	}

	@Override
    public String getAttribute(String key) {
		// NOT SUPPORTED FOR NOW
		return null;
	}

	@Override
    public int getExitValue() throws DebugException {
		if (isTerminated()) {
			return exitValue;
		}
		throw new DebugException(
				new Status(
						IStatus.ERROR,
						DebugPlugin.getUniqueIdentifier(),
						DebugException.TARGET_REQUEST_FAILED,
						DebugCoreMessages.RuntimeProcess_Exit_value_not_available_until_process_terminates__1,
						null));
	}

	@Override
    public void eventFired(EventObject event) {
		if (event instanceof ProcessStateChangeEvent) {
			ProcessStateChangeEvent pscEvent = (ProcessStateChangeEvent) event;
			if (pscEvent.getEventId().equals(
					ProcessStateChangeEvent.EVENT_PROCESS_CREATED)) {
				if ((pscEvent.getSource() instanceof ProcessContext)) {
					if (prLauncher != null && prLauncher.getAdapter(ProcessContext.class) == pscEvent.getSource())
						context = (ProcessContext) pscEvent.getSource();
				}
			} else if (pscEvent.getEventId().equals(
					ProcessStateChangeEvent.EVENT_PROCESS_TERMINATED)) {
				if ((pscEvent.getSource() instanceof ProcessContext)) {
					if (((ProcessContext) pscEvent.getSource()).getID().equals(
							context.getID())) {
						exitValue = pscEvent.getExitCode();
						terminated = true;
						fireTerminateEvent();
					}
				}
			}
		}

	}

	/**
	 * Fires a creation event.
	 */
	protected void fireCreationEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
	}

	/**
	 * Fires the given debug event.
	 *
	 * @param event
	 *            debug event to fire
	 */
	protected void fireEvent(DebugEvent event) {
		DebugPlugin manager = DebugPlugin.getDefault();
		if (manager != null) {
			manager.fireDebugEventSet(new DebugEvent[] { event });
		}
	}

	/**
	 * Fires a terminate event.
	 */
	protected void fireTerminateEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
	}

	/**
	 * Fires a change event.
	 */
	protected void fireChangeEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.CHANGE));
	}

}
