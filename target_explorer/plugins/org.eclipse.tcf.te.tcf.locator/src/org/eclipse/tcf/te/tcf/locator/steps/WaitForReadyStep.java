/*******************************************************************************
 * Copyright (c) 2013, 2015, 2017 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.locator.steps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.utils.ProgressHelper;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.channelmanager.OpenChannelException;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelUpdateService;
import org.eclipse.tcf.te.tcf.locator.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.utils.SimulatorUtils;

/**
 * WaitForReadyStep
 */
public class WaitForReadyStep extends AbstractPeerNodeStep {

	public static final String PARAMETER_NO_TIMEOUT = "noTimeout"; //$NON-NLS-1$
	public static final String PARAMETER_REPEAT_TIMES = "repeatTimes"; //$NON-NLS-1$

	/**
	 * Constructor.
	 */
	public WaitForReadyStep() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(final IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, final ICallback callback) {
		final IPeerNode peerNode = getActivePeerModelContext(context, data, fullQualifiedId);
		String param = System.getProperty(getClass().getSimpleName() + "_" + PARAMETER_NO_TIMEOUT); //$NON-NLS-1$
		if (param == null) param = getParameters().get(PARAMETER_NO_TIMEOUT);
		boolean paramBool = false;
		if (param != null) {
			try {
				paramBool = Boolean.parseBoolean(param);
			}
			catch (Exception e) {
			}
		}
		final boolean noTimeout;
		final int repeatTimes;

		String repeatTimesParam = System.getProperty(getClass().getSimpleName() + "_" + PARAMETER_REPEAT_TIMES); //$NON-NLS-1$
		if (repeatTimesParam == null) repeatTimesParam = getParameters().get(PARAMETER_REPEAT_TIMES);
		int repeatTimesInt = 0;
		if (repeatTimesParam != null) {
			try {
				repeatTimesInt = Integer.parseInt(repeatTimesParam);
			}
			catch (Exception e) {
			}
		}
		if (repeatTimesInt > 0) {
			noTimeout = false;
			repeatTimes = repeatTimesInt;
		} else {
			noTimeout = paramBool;
			/* Default Value: 10 times (means reconnect try 10 times) */
			repeatTimes = 10;
		}

		if (peerNode != null && !Boolean.getBoolean("WaitForReadyStep.skip")) { //$NON-NLS-1$
			Protocol.invokeLater(new Runnable() {
				final Runnable thisRunnable = this;
				// set repeat count to 'repeatTimes' if real target is used
				int totalWork = getTotalWork(context, data);
				SimulatorUtils.Result result = SimulatorUtils.getSimulatorService(getActivePeerModelContext(context, data, fullQualifiedId));
				final boolean isSimulatorRunning = result != null;
				int refreshCount = isSimulatorRunning ? 0 : totalWork-repeatTimes-1;
				final AtomicReference<Throwable> lastError = new AtomicReference<Throwable>();

				@Override
				public void run() {
					if (ProgressHelper.isCancel(WaitForReadyStep.this, monitor, callback)) {
						return;
					}
					else if (!noTimeout && refreshCount >= totalWork) {
						@SuppressWarnings("synthetic-access")
                        String message = NLS.bind(Messages.WaitForReadyStep_error_timeout, getActivePeerContext(context, data, fullQualifiedId).getName());
						if (lastError.get() != null) {
							String cause = lastError.get().getLocalizedMessage();
							if (cause == null || "".equals(cause.trim())) cause = lastError.get().getClass().getName(); //$NON-NLS-1$
							if (!cause.contains(lastError.get().getClass().getName())) cause += " (" + lastError.get().getClass().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
							message += NLS.bind(Messages.WaitForReadyStep_error_timeout_cause, cause);
						}
						callback(data, fullQualifiedId, callback, StatusHelper.getStatus(new TimeoutException(message)), null);
					}
					else {
						// Try to open a channel to the target and check for errors
						Tcf.getChannelManager().openChannel(peerNode.getPeer(), null, new IChannelManager.DoneOpenChannel() {
							@Override
							public void doneOpenChannel(final Throwable error, final IChannel channel) {
								if (ProgressHelper.isCancel(WaitForReadyStep.this, monitor, callback)) {
									return;
								}

								Object exitError = peerNode.getProperty(IPeerNodeProperties.PROP_EXIT_ERROR);
								if (exitError instanceof Throwable) {
									callback(data, fullQualifiedId, callback, StatusHelper.getStatus((Throwable)exitError), null);
									return;
								}

								IStatus status = null;

								// If the channel open succeeded, we are done
								if (error == null && channel != null && channel.getState() == IChannel.STATE_OPEN) {
									StepperAttributeUtil.setProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data, channel, true);
									status = Status.OK_STATUS;
								}

								// If we have an OK status, we are done
								if (status != null && status.isOK()) {
									// Get the local service
									List<String> localServices = new ArrayList<String>(channel.getLocalServices());
									// Get the remote services
									List<String> remoteServices = new ArrayList<String>(channel.getRemoteServices());

									// Sort the service lists
									Collections.sort(localServices);
									Collections.sort(remoteServices);

									// Update the services
									IPeerModelUpdateService updateService = peerNode.getModel().getService(IPeerModelUpdateService.class);
									updateService.updatePeerServices(peerNode, localServices, remoteServices);

									channel.addChannelListener(peerNode);

									callback(data, fullQualifiedId, callback, status, null);
									return;
								}

								// ANY exceptions, except for OpenChannelExceptions, are reported to the user and breaks the wait immediately
								if (error != null && !(error instanceof OpenChannelException)) {
									callback(data, fullQualifiedId, callback, StatusHelper.getStatus(error), null);
									return;
								}

								// Remember the last error for use later
								lastError.set(error instanceof OpenChannelException ? ((OpenChannelException) error).getError() : error);

								// Try again until timed out
								refreshCount++;
								ProgressHelper.worked(monitor, 1);
								int delay = refreshCount < 20 ? 500 : 1000;
								monitor.setTaskName(getLabel());
								Protocol.invokeLater(delay, thisRunnable);
							}
						}, monitor);
					}
				}
			});
		}
		else {
			callback(data, fullQualifiedId, callback, Status.OK_STATUS, null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep#rollback(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.core.runtime.IStatus, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void rollback(final IStepContext context, final IPropertiesContainer data, final IStatus status, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, final ICallback callback) {
		final IPeer peer = getActivePeerContext(context, data, fullQualifiedId);
		final IPeerNode peerNode = getActivePeerModelContext(context, data, fullQualifiedId);

		Runnable runnable = new Runnable() {
			@SuppressWarnings("synthetic-access")
            @Override
			public void run() {
				peerNode.setProperty(IPeerNodeProperties.PROP_EXIT_ERROR, null);
				Tcf.getChannelManager().shutdown(peer);
				WaitForReadyStep.super.rollback(context, data, status, fullQualifiedId, monitor, callback);
			}
		};

		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeLater(runnable);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep#getTotalWork(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public int getTotalWork(IStepContext context, IPropertiesContainer data) {
	    return 100;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep#getCancelTimeout()
	 */
	@Override
	public int getCancelTimeout() {
	    return 500;
	}
}
