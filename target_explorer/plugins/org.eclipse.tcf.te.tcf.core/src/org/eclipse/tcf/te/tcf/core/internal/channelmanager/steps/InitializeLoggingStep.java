/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.internal.channelmanager.steps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.tcf.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.core.events.ChannelEvent;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;
import org.eclipse.tcf.te.tcf.core.steps.AbstractPeerStep;

/**
 * Initialize channel communication logging step implementation.
 */
public class InitializeLoggingStep extends AbstractPeerStep {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(context);
		Assert.isNotNull(data);
		Assert.isNotNull(fullQualifiedId);
		Assert.isNotNull(monitor);

		IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);
		if (channel == null) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "Channel to target not available.")); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(final IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, final ICallback callback) {
		Assert.isNotNull(context);
		Assert.isNotNull(data);
		Assert.isNotNull(fullQualifiedId);
		Assert.isNotNull(monitor);
		Assert.isNotNull(callback);

		final IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);
		Assert.isNotNull(channel);
		final String logname = StepperAttributeUtil.getStringProperty(ITcfStepAttributes.ATTR_LOG_NAME, fullQualifiedId, data);

		if (channel.getState() == IChannel.STATE_OPEN) {
			// Query the remote services from the channel before sending the "open" event
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					// Get the remote services from the channel
					List<String> remoteServices = new ArrayList<String>(channel.getRemoteServices());
					Collections.sort(remoteServices);

					String buffer = remoteServices.toString();
					buffer = buffer.replaceAll("\\[", "").replaceAll("\\]", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

					IPropertiesContainer eventData = new PropertiesContainer();
					eventData.setProperty(ChannelEvent.PROP_MESSAGE, buffer.toString());
					eventData.setProperty(ChannelEvent.PROP_LOG_NAME, logname);

					ChannelEvent event = new ChannelEvent(InitializeLoggingStep.this, channel, ChannelEvent.TYPE_SERVICS, eventData);
					EventManager.getInstance().fireEvent(event);

					// Get the data to send the "open" event
					boolean forceNew = StepperAttributeUtil.getBooleanProperty(IChannelManager.FLAG_FORCE_NEW, fullQualifiedId, data);
					boolean noValueAdd = StepperAttributeUtil.getBooleanProperty(IChannelManager.FLAG_NO_VALUE_ADD, fullQualifiedId, data);
					boolean noPathMap = StepperAttributeUtil.getBooleanProperty(IChannelManager.FLAG_NO_PATH_MAP, fullQualifiedId, data);

					// Log successfully opened channels
					String message = forceNew ? "Private" : "Shared"; //$NON-NLS-1$ //$NON-NLS-2$
					if (noValueAdd) message += ", No Value Add"; //$NON-NLS-1$
					if (noPathMap) message += ", Not Applying Path Map"; //$NON-NLS-1$

					eventData = new PropertiesContainer();
					eventData.setProperty(ChannelEvent.PROP_MESSAGE, message);
					eventData.setProperty(ChannelEvent.PROP_LOG_NAME, logname);

					event = new ChannelEvent(InitializeLoggingStep.this, channel, ChannelEvent.TYPE_OPEN, eventData);
					EventManager.getInstance().fireEvent(event);

					callback(data, fullQualifiedId, callback, Status.OK_STATUS, null);
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeLater(runnable);
		} else {
			callback(data, fullQualifiedId, callback, Status.OK_STATUS, null);
		}
	}


}
