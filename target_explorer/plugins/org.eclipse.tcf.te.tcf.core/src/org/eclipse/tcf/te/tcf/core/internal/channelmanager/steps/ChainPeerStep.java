/*******************************************************************************
 * Copyright (c) 2014, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.core.internal.channelmanager.steps;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.core.channelmanager.OpenChannelException;
import org.eclipse.tcf.te.tcf.core.events.ChannelEvent;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;
import org.eclipse.tcf.te.tcf.core.steps.AbstractPeerStep;

/**
 * ChainPeerStep
 */
public class ChainPeerStep extends AbstractPeerStep {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(context);
		Assert.isNotNull(data);
		Assert.isNotNull(fullQualifiedId);
		Assert.isNotNull(monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, IProgressMonitor monitor, final ICallback callback) {
		Assert.isNotNull(context);
		Assert.isNotNull(data);
		Assert.isNotNull(fullQualifiedId);
		Assert.isNotNull(monitor);
		Assert.isNotNull(callback);

		final AtomicReference<IChannel> channel = new AtomicReference<IChannel>((IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data));
		final IPeer peer = getActivePeerContext(context, data, fullQualifiedId);
		final String logname = StepperAttributeUtil.getStringProperty(ITcfStepAttributes.ATTR_LOG_NAME, fullQualifiedId, data);

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				IChannel c = channel.get();

				// If the channel is not yet opened, open it now.
				// Otherwise redirect the channel to the next peer.
				if (c == null) {
					c = peer.openChannel();
					channel.set(c);

					String message = "to " + peer.getID(); //$NON-NLS-1$

					IPropertiesContainer eventData = new PropertiesContainer();
					eventData.setProperty(ChannelEvent.PROP_MESSAGE, message);
					eventData.setProperty(ChannelEvent.PROP_LOG_NAME, logname);

					ChannelEvent event = new ChannelEvent(ChainPeerStep.this, c, ChannelEvent.TYPE_OPENING, eventData);
					EventManager.getInstance().fireEvent(event);
				} else {
					String id = peer.getAttributes().get(IPeer.ATTR_TRANSPORT_NAME) + ":" + peer.getAttributes().get(IPeer.ATTR_IP_HOST) + ":" + peer.getAttributes().get(IPeer.ATTR_IP_PORT); //$NON-NLS-1$ //$NON-NLS-2$
					id = id.equalsIgnoreCase(peer.getID()) ? peer.getID() : (id + " (" + peer.getID() + ")");  //$NON-NLS-1$ //$NON-NLS-2$
					String message = c.getRemotePeer().getID() + " --> " + id; //$NON-NLS-1$

					IPropertiesContainer eventData = new PropertiesContainer();
					eventData.setProperty(ChannelEvent.PROP_MESSAGE, message);
					eventData.setProperty(ChannelEvent.PROP_LOG_NAME, logname);

					ChannelEvent event = new ChannelEvent(ChainPeerStep.this, c, ChannelEvent.TYPE_REDIRECT, eventData);
					EventManager.getInstance().fireEvent(event);

					c.redirect(peer.getAttributes());
				}

				// At this point, channel must not be null and
				// channel.get and c must be the same
				Assert.isNotNull(c);
				Assert.isTrue(c.equals(channel.get()));

				c.addChannelListener(new IChannel.IChannelListener() {
					@Override
					public void onChannelOpened() {
						channel.get().removeChannelListener(this);
						StepperAttributeUtil.setProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data, channel.get(), true);
						callback(data, fullQualifiedId, callback, Status.OK_STATUS, null);
					}

					@Override
					public void onChannelClosed(Throwable error) {
						// Clean Logs when closed channels
						ChannelEvent event = new ChannelEvent(ChainPeerStep.this, channel.get(), ChannelEvent.TYPE_CLOSE, null);
						EventManager.getInstance().fireEvent(event);
                    
						// Remove ourself as listener from the channel
						channel.get().removeChannelListener(this);
						// The error is repackaged
						if (error != null) {
							error = new OpenChannelException(error);
						}
						// Invoke the callback
						callback(data, fullQualifiedId, callback, StatusHelper.getStatus(error), null);
					}

					@Override
					public void congestionLevel(int level) {
					}
				});
			}
		};

		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeLater(getProxyDelay(), runnable);
	}
	
	private long getProxyDelay() {
		int tcfProxyDelay = 1000;
		String tcfRedirectDelayString = System.getProperty("tcfProxyDelay");
		if ( null != tcfRedirectDelayString ) {
			try {
				tcfProxyDelay = Integer.parseInt(tcfRedirectDelayString);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (tcfProxyDelay < 10) {
			tcfProxyDelay = 1000;
		}
				
		return tcfProxyDelay;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep#rollback(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.core.runtime.IStatus, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void rollback(final IStepContext context, final IPropertiesContainer data, final IStatus status, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, final ICallback callback) {
		final IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);

		if (channel != null && channel.getState() != IChannel.STATE_CLOSED) {
			Runnable runnable = new Runnable() {
				@SuppressWarnings("synthetic-access")
                @Override
				public void run() {
					channel.close();
					ChainPeerStep.super.rollback(context, data, status, fullQualifiedId, monitor, callback);
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeLater(runnable);
		} else {
			super.rollback(context, data, status, fullQualifiedId, monitor, callback);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep#getCancelTimeout()
	 */
	@Override
	public int getCancelTimeout() {
	    return 500;
	}
}
