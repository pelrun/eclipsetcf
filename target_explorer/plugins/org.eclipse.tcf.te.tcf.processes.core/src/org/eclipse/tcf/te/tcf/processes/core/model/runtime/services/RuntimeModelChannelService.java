/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.runtime.services;


import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelChannelService;
import org.eclipse.tcf.te.tcf.core.model.services.AbstractModelService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.core.model.runtime.listener.RuntimeModelProcessServiceListener;
import org.eclipse.tcf.te.tcf.processes.core.model.runtime.listener.RuntimeModelRunControlServiceListener;

/**
 * Runtime model channel service implementation.
 */
public class RuntimeModelChannelService extends AbstractModelService<IRuntimeModel> implements IModelChannelService {
	// Reference to the channel instance
	/* default */ IChannel channel;
	// Reference to the process service listener
	/* default */ IProcesses.ProcessesListener serviceListener;
	// Reference to the run control service listener
	/* default */ IRunControl.RunControlListener runControlListener;

	/**
	 * Constructor.
	 *
	 * @param model The parent model. Must not be <code>null</code>.
	 */
	public RuntimeModelChannelService(IRuntimeModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelChannelService#getChannel()
	 */
	@Override
	public IChannel getChannel() {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		return channel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelChannelService#openChannel(org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelChannelService.DoneOpenChannel)
	 */
	@Override
	public void openChannel(final DoneOpenChannel done) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// If a channel is associated which is in open state, return it
		if (channel != null && channel.getState() == IChannel.STATE_OPEN) {
			done.doneOpenChannel(null, channel);
			return;
		}

		// Get the peer model node
		IPeerNode node = getModel().getPeerNode();
		if (node != null) {
			// Open a new channel to the remote peer
			Tcf.getChannelManager().openChannel(node.getPeer(), null, new IChannelManager.DoneOpenChannel() {

				@Override
				public void doneOpenChannel(Throwable error, IChannel channel) {
					// Remember the channel instance
					RuntimeModelChannelService.this.channel = channel;
					// Attach the process service listener instance
					if (error == null && serviceListener == null) {
						IProcesses service = channel.getRemoteService(IProcesses.class);
						if (service != null) {
							serviceListener = new RuntimeModelProcessServiceListener(getModel());
							service.addListener(serviceListener);
						}
					}
					// Attach the run control service listener instance
					if (error == null && runControlListener == null) {
						IRunControl service = channel.getRemoteService(IRunControl.class);
						if (service != null) {
							runControlListener = new RuntimeModelRunControlServiceListener(getModel());
							service.addListener(runControlListener);
						}
					}

					done.doneOpenChannel(error, channel);
				}
			});
		}
		else {
			done.doneOpenChannel(new NullPointerException("Peer model node is null"), null); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelChannelService#closeChannel()
	 */
	@Override
	public void closeChannel() {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		if (channel != null) {
			if (serviceListener != null) {
				IProcesses service = channel.getRemoteService(IProcesses.class);
				if (service != null) { service.removeListener(serviceListener); serviceListener = null; }
			}

			Tcf.getChannelManager().closeChannel(channel);
			channel = null;
		}
	}
}
