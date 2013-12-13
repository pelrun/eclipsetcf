/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.locator.steps;

import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.utils.ProgressHelper;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.nls.Messages;

/**
 * WaitForReadyStep
 */
public class WaitForReadyStep extends AbstractPeerNodeStep {

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

		if (peerNode != null && !Boolean.getBoolean("WaitForReadyStep.skip")) { //$NON-NLS-1$
			Protocol.invokeLater(new Runnable() {
				final Runnable thisRunnable = this;
				int refreshCount = 0;
				@Override
				public void run() {
					if (ProgressHelper.isCancel(WaitForReadyStep.this, monitor, callback)) {
						return;
					}
					else if (refreshCount >= getTotalWork(context, data)) {
						@SuppressWarnings("synthetic-access")
                        String message = NLS.bind(Messages.WaitForReadyStep_error_timeout, getActivePeerContext(context, data, fullQualifiedId).getName());
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
								IStatus status = null;

								// If the channel open succeeded, we are done
								if (error == null && channel != null && channel.getState() == IChannel.STATE_OPEN) {
									status = Status.OK_STATUS;
								}

								// Close the channel right away
								if (channel != null) Tcf.getChannelManager().closeChannel(channel);

								// If we have an OK status, we are done
								if (status != null && status.isOK()) {
									callback(data, fullQualifiedId, callback, status, null);
									return;
								}

								// License errors are reported to the user and breaks the wait immediately
								if (error != null) {
									String message = error.getLocalizedMessage();
									if (message != null && (message.contains("LMAPI error occured:") //$NON-NLS-1$
												|| message.contains("Failed to read output from value-add"))) { //$NON-NLS-1$
										callback(data, fullQualifiedId, callback, StatusHelper.getStatus(error), null);
										return;
									}
								}

								// Try again until timed out
								refreshCount++;
								ProgressHelper.worked(monitor, 1);
								Protocol.invokeLater(refreshCount < 20 ? 500 : 1000, thisRunnable);
							}
						});
					}
				}
			});
		}
		else {
			callback(data, fullQualifiedId, callback, Status.OK_STATUS, null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep#getTotalWork(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public int getTotalWork(IStepContext context, IPropertiesContainer data) {
	    return 100;
	}
}
