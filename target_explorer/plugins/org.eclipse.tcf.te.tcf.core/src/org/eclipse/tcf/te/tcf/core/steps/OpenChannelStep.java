/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.steps;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.utils.ProgressHelper;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;

/**
 * Open channel step implementation.
 */
public class OpenChannelStep extends AbstractPeerStep {

	/**
	 * Constructor.
	 */
	public OpenChannelStep() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IExtendedStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, IProgressMonitor monitor, final ICallback callback) {
		Tcf.getChannelManager().openChannel(getActivePeerContext(context, data, fullQualifiedId), null, new IChannelManager.DoneOpenChannel() {
			@Override
			public void doneOpenChannel(final Throwable error, final IChannel channel) {
				StepperAttributeUtil.setProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId.getParentId(), data, channel);
				callback(data, fullQualifiedId, callback, StatusHelper.getStatus(error), null);
			}
		}, monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep#rollback(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.core.runtime.IStatus, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void rollback(IStepContext context, IPropertiesContainer data, IStatus status, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor, ICallback callback) {
		IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);
		if (channel != null && channel.getState() != IChannel.STATE_CLOSED) {
			ProgressHelper.setSubTaskName(monitor, "Closing TCF channel"); //$NON-NLS-1$
			Tcf.getChannelManager().closeChannel(channel, monitor);
		}
		super.rollback(context, data, status, fullQualifiedId, monitor, callback);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep#getCancelTimeout()
	 */
	@Override
	public int getCancelTimeout() {
	    return 500;
	}
}
