/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.services;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.concurrent.Rendezvous;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.DoneOpenChannel;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.exceptions.TCFChannelException;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.BlockingFileSystemProxy;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * Operation is the base class of file system operation classes.
 * @see IOperation
 */
public class Operation  {
	// The default timeout waiting for blocked invocations.
	public static final long DEFAULT_TIMEOUT = 60000L;


	private Operation() {
	}

	/**
	 * Open a channel connected to the target represented by the peer.
	 *
	 * @return The channel or null if the operation fails.
	 */
	public static IChannel openChannel(final IPeer peer) throws TCFChannelException {
		final TCFChannelException[] errors = new TCFChannelException[1];
		final IChannel[] channels = new IChannel[1];
		final Rendezvous rendezvous = new Rendezvous();
		Tcf.getChannelManager().openChannel(peer, null, new DoneOpenChannel() {
			@Override
			public void doneOpenChannel(Throwable error, IChannel channel) {
				if (error != null) {
					if (error instanceof ConnectException) {
						String message = NLS.bind(Messages.Operation_NotResponding, peer.getID());
						errors[0] = new TCFChannelException(IStatus.ERROR, message);
					}
					else if(!(error instanceof OperationCanceledException)) {
						String message = NLS.bind(Messages.Operation_OpeningChannelFailureMessage, peer.getID(), error.getMessage());
						errors[0] = new TCFChannelException(IStatus.OK, message, error);
					}
				}
				else {
					channels[0] = channel;
				}
				rendezvous.arrive();
			}
		});
		try {
			rendezvous.waiting(DEFAULT_TIMEOUT);
		}
		catch(TimeoutException e) {
			throw new TCFChannelException(IStatus.ERROR, Messages.Operation_TimeoutOpeningChannel);
		}
		if (errors[0] != null) {
			throw errors[0];
		}
		return channels[0];
	}

	/**
	 * Get a blocking file system service from the channel. The
	 * returned file system service is a service that delegates the
	 * method call to the file system proxy. If the method returns
	 * asynchronously with a callback, it will block the call until
	 * the callback returns.
	 * <p>
	 * <em>Note: All the method of the returned file system
	 * service must be called outside of the dispatching thread.</em>
	 *
	 * @param channel The channel to get the file system service.
	 * @return The blocking file system service.
	 */
	public static IFileSystem getBlockingFileSystem(final IChannel channel) {
		if(Protocol.isDispatchThread()) {
			IFileSystem service = channel.getRemoteService(IFileSystem.class);
			return new BlockingFileSystemProxy(service);
		}
		final IFileSystem[] service = new IFileSystem[1];
		Protocol.invokeAndWait(new Runnable(){
			@Override
            public void run() {
				service[0] = getBlockingFileSystem(channel);
            }});
		return service[0];
	}
}
