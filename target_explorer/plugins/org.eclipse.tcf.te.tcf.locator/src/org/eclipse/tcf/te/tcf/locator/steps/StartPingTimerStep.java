/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.steps;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IDiagnostics;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;
import org.eclipse.tcf.te.tcf.locator.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.utils.SimulatorUtils;

/**
 * Start ping timer step implementation.
 */
public class StartPingTimerStep extends AbstractPeerNodeStep {

	/**
	 * Constructor.
	 */
	public StartPingTimerStep() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IExtendedStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(final IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor) throws CoreException {
		IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);
		if (channel == null || channel.getState() != IChannel.STATE_OPEN) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "missing or closed channel")); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(final IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, final ICallback callback) {
	    SimulatorUtils.Result result = SimulatorUtils.getSimulatorService(getActivePeerModelContext(context, data, fullQualifiedId));

		if (result == null) {
			final IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					final IPeerNode peerNode = getActivePeerModelContext(context, data, fullQualifiedId);
					final int pingInterval;
					final int pingTimeout;
					Map<String, String> attrs = peerNode.getPeer().getAttributes();

					int interval = 10000;
					if (attrs.containsKey(IPeerNodeProperties.PROP_PING_INTERVAL)) {
						try {
							interval = Integer.parseInt(attrs.get(IPeerNodeProperties.PROP_PING_INTERVAL));
							interval = interval * 1000;
						}
						catch (NumberFormatException nfe) {
						}
					}
					pingInterval = interval;

					int timeout = 10000;
					if (attrs.containsKey(IPeerNodeProperties.PROP_PING_TIMEOUT)) {
						try {
							timeout = Integer.parseInt(attrs.get(IPeerNodeProperties.PROP_PING_TIMEOUT));
							timeout = timeout * 1000;
						}
						catch (NumberFormatException nfe) {
						}
					}
					pingTimeout = timeout;

					if (pingInterval > 0 && pingTimeout > 0) {
						final String name = peerNode.getName();
						final IDiagnostics diagnostics = channel.getRemoteService(IDiagnostics.class);
						if (diagnostics != null) {
							final Timer pingTimer = new Timer(name + " ping"); //$NON-NLS-1$
							final TimerTask timeoutTask = new TimerTask() {
								@Override
	                            public void run() {
									try {
										pingTimer.cancel();
										channel.close();
									}
									catch (Throwable e) {
									}
								}
							};
							TimerTask pingTask = new TimerTask() {
								final AtomicBoolean running = new AtomicBoolean(false);
								@Override
								public void run() {
									try {
										if (!running.get()) {
											running.set(true);
											Protocol.invokeLater(new Runnable() {
												@Override
												public void run() {
													try {
														if (peerNode.getConnectState() == IConnectable.STATE_CONNECTED) {
															diagnostics.echo("ping", new IDiagnostics.DoneEcho() { //$NON-NLS-1$
																@Override
																public void doneEcho(IToken token, Throwable error, String s) {
																	timeoutTask.cancel();
																	running.set(false);
																	if (error != null) {
																		pingTimer.cancel();
																	}
																}
															});
															pingTimer.schedule(timeoutTask, pingTimeout);
														}
														else {
															pingTimer.cancel();
														}
													}
													catch (Throwable e) {}
												}
											});
										}
									}
									catch (Throwable e) {}
								}
							};
							pingTimer.schedule(pingTask, pingInterval, pingInterval);
						}
						else if (Platform.inDebugMode()) {
							Platform.getLog(CoreBundleActivator.getDefault().getBundle()).log(new Status(IStatus.WARNING,
											CoreBundleActivator.getUniqueIdentifier(),
											NLS.bind(Messages.StartPingTimerStep_warning_noDiagnosticsService, name)));
						}
					}
				}
			});
		}

		callback(data, fullQualifiedId, callback, Status.OK_STATUS, null);
	}
}
