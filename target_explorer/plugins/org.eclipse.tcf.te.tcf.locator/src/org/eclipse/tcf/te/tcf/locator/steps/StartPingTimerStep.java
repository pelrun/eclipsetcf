/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
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
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;
import org.eclipse.tcf.te.tcf.locator.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.locator.interfaces.IStepAttributes;
import org.eclipse.tcf.te.tcf.locator.interfaces.ITracing;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
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

	    boolean startClientPing = StepperAttributeUtil.getBooleanProperty(IStepAttributes.ATTR_START_CLIENT_PING, fullQualifiedId, data);

		if (result == null && startClientPing) {
			final IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					final IPeerNode peerNode = getActivePeerModelContext(context, data, fullQualifiedId);
					final int pingInterval;
					final int pingTimeout;
					Map<String, String> attrs = peerNode.getPeer().getAttributes();

					int interval = 10000;
					if (attrs.containsKey(IPeerProperties.PROP_PING_INTERVAL)) {
						try {
							interval = Integer.parseInt(attrs.get(IPeerProperties.PROP_PING_INTERVAL));
							interval = interval * 1000;
						}
						catch (NumberFormatException nfe) {
						}
					}
					pingInterval = interval;

					int timeout = 10000;
					if (attrs.containsKey(IPeerProperties.PROP_PING_TIMEOUT)) {
						try {
							timeout = Integer.parseInt(attrs.get(IPeerProperties.PROP_PING_TIMEOUT));
							timeout = timeout * 1000;
						}
						catch (NumberFormatException nfe) {
						}
					}
					pingTimeout = timeout;

					if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_PING)) {
						CoreBundleActivator.getTraceHandler().trace("Interval="+pingInterval+"ms Timeout="+pingTimeout+"ms", ITracing.ID_TRACE_PING, StartPingTimerStep.this); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}

					if (pingInterval > 0 && pingTimeout > 0) {
						final String name = peerNode.getName();
						final IDiagnostics diagnostics = channel.getRemoteService(IDiagnostics.class);
						if (diagnostics != null) {
							final Timer pingTimer = new Timer(name + " ping"); //$NON-NLS-1$
							TimerTask pingTask = new TimerTask() {
								final AtomicBoolean running = new AtomicBoolean(false);
								@Override
								public void run() {
									try {
										if (!running.get()) {
											running.set(true);
											Protocol.invokeLater(new Runnable() {
												TimerTask timeoutTask = null;
												long startTime = 0;
												@Override
												public void run() {
													try {
														if (peerNode.getConnectState() == IConnectable.STATE_CONNECTED) {
															if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_PING)) {
																startTime = System.currentTimeMillis();
																CoreBundleActivator.getTraceHandler().trace("Send ping.", ITracing.ID_TRACE_PING, StartPingTimerStep.this); //$NON-NLS-1$
															}
															diagnostics.echo("ping", new IDiagnostics.DoneEcho() { //$NON-NLS-1$
																@Override
																public void doneEcho(IToken token, Throwable error, String s) {
																	if (!running.get()) {
																		return;
																	}
																	if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_PING)) {
																		long endTime = System.currentTimeMillis();
																		CoreBundleActivator.getTraceHandler().trace("Received ping after "+(endTime-startTime)+"ms.", ITracing.ID_TRACE_PING, StartPingTimerStep.this); //$NON-NLS-1$ //$NON-NLS-2$
																	}
																	if (timeoutTask != null) {
																		timeoutTask.cancel();
																		timeoutTask = null;
																	}
																	running.set(false);
																	if (error != null) {
																		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_PING)) {
																			CoreBundleActivator.getTraceHandler().trace("Received error '"+error.getMessage()+"'.", ITracing.ID_TRACE_PING, StartPingTimerStep.this); //$NON-NLS-1$ //$NON-NLS-2$
																		}
																		pingTimer.cancel();
																	}
																}
															});
															timeoutTask = new TimerTask() {
																@Override
									                            public void run() {
																	if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_PING)) {
																		long endTime = System.currentTimeMillis();
																		CoreBundleActivator.getTraceHandler().trace("Timeout after "+(endTime-startTime)+"ms.", ITracing.ID_TRACE_PING, StartPingTimerStep.this); //$NON-NLS-1$ //$NON-NLS-2$
																	}
																	try {
																		pingTimer.cancel();
																		running.set(false);
																		Protocol.invokeLater(new Runnable() {
																			@Override
																			public void run() {
																				if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_PING)) {
																					CoreBundleActivator.getTraceHandler().trace("Close channel.", ITracing.ID_TRACE_PING, StartPingTimerStep.this); //$NON-NLS-1$
																				}
																				channel.close();
																			}
																		});
																	}
																	catch (Throwable e) {
																		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_PING)) {
																			CoreBundleActivator.getTraceHandler().trace("Error '"+e.getMessage()+"'.", ITracing.ID_TRACE_PING, StartPingTimerStep.this); //$NON-NLS-1$ //$NON-NLS-2$
																		}
																	}
																}
															};
															pingTimer.schedule(timeoutTask, pingTimeout);
														}
														else {
															if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_PING)) {
																CoreBundleActivator.getTraceHandler().trace("Connection no longer connected - cancel.", ITracing.ID_TRACE_PING, StartPingTimerStep.this); //$NON-NLS-1$
															}
															pingTimer.cancel();
														}
													}
													catch (Throwable e) {
														if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_PING)) {
															CoreBundleActivator.getTraceHandler().trace("Error '"+e.getMessage()+"'.", ITracing.ID_TRACE_PING, StartPingTimerStep.this); //$NON-NLS-1$ //$NON-NLS-2$
														}
													}
												}
											});
										}
									}
									catch (Throwable e) {
										if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_PING)) {
											CoreBundleActivator.getTraceHandler().trace("Error '"+e.getMessage()+"'.", ITracing.ID_TRACE_PING, StartPingTimerStep.this); //$NON-NLS-1$ //$NON-NLS-2$
										}
									}
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
