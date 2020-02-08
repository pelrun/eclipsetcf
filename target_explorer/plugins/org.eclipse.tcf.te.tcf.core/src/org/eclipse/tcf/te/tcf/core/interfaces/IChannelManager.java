/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.interfaces;

import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.services.IStreams;

/**
 * TCF channel manager public API declaration.
 */
public interface IChannelManager extends IAdaptable {

	/**
	 * If set to <code>true</code>, a new and not reference counted channel is opened.
	 * <p>
	 * All channels opened by the channel manager must be closed by the channel managers
	 * {@link #closeChannel(IChannel)} API.
	 * <p>
	 * If not present in the flags map passed in to open channel, the default value is
	 * <code>false</code>.
	 */
	public static final String FLAG_FORCE_NEW = "channel.forceNew"; //$NON-NLS-1$

	/**
	 * If set to <code>true</code>, a new and not reference counted channel is opened,
	 * and no value add is launched and associated with the channel. This option should
	 * be used with extreme caution.
	 * <p>
	 * All channels opened by the channel manager must be closed by the channel managers
	 * {@link #closeChannel(IChannel)} API.
	 * <p>
	 * If not present in the flags map passed in to open channel, the default value is
	 * <code>false</code>.
	 */
	public static final String FLAG_NO_VALUE_ADD = "channel.noValueAdd"; //$NON-NLS-1$

	/**
	 * If set to <code>true</code>, a new and not reference counted channel is opened,
	 * and the configured path map is not auto applied to the opened channel.
	 * <p>
	 * All channels opened by the channel manager must be closed by the channel managers
	 * {@link #closeChannel(IChannel)} API.
	 * <p>
	 * If not present in the flags map passed in to open channel, the default value is
	 * <code>false</code>.
	 */
	public static final String FLAG_NO_PATH_MAP = "channel.noPathMap"; //$NON-NLS-1$

	/**
	 * Client call back interface for openChannel(...).
	 */
	interface DoneOpenChannel {
		/**
		 * Called when the channel fully opened or failed to open.
		 * <p>
		 * <b>Note:</b> If error is of type {@link OperationCanceledException}, than it signals that
		 * the channel got closed normally while still in state {@link IChannel#STATE_OPENING}. Clients
		 * should handle the case explicitly if necessary.
		 *
		 * @param error The error description if operation failed, <code>null</code> if succeeded.
		 * @param channel The channel object or <code>null</code>.
		 */
		void doneOpenChannel(Throwable error, IChannel channel);
	}

	/**
	 * Opens a new channel to communicate with the given peer.
	 * <p>
	 * Channels opened by the channel manager must be closed by the channel managers
	 * {@link #closeChannel(IChannel)} API.
	 * <p>
	 * The method can be called from any thread context, the client done callback
	 * is always invoked within the TCF event dispatch thread.
	 *
	 * @param peer The peer. Must not be <code>null</code>.
	 * @param flags Map containing the flags to parameterize the channel opening, or <code>null</code>.
	 * @param done The client callback. Must not be <code>null</code>.
	 */
	public void openChannel(IPeer peer, Map<String, Boolean> flags, DoneOpenChannel done);

	public void openChannel(IPeer peer, Map<String, Boolean> flags, DoneOpenChannel done, IProgressMonitor monitor);

	/**
	 * Returns the shared channel instance for the given peer. Channels retrieved using this
	 * method cannot be closed by the caller.
	 * <p>
	 * Callers of this method are expected to test for the current channel state themselves.
	 * <p>
	 * The method can be called from any thread context.
	 *
	 * @param peer The peer. Must not be <code>null</code>.
	 * @return The channel instance or <code>null</code>.
	 */
	public IChannel getChannel(IPeer peer);

	/**
	 * Closes the given channel.
	 * <p>
	 * If the given channel is a reference counted channel, the channel will be closed if the reference counter
	 * reaches 0. For non reference counted channels, the channel is closed immediately.
	 * <p>
	 * <b>Note:</b> Closing a channel is an asynchronous operation and the {@link #closeChannel(IChannel)}
	 * method can return while closing the channel is still in progress.
	 * <p>
	 * The method can be called from any thread context.
	 *
	 * @param channel The channel. Must not be <code>null</code>.
	 */
	public void closeChannel(IChannel channel);

	public void closeChannel(IChannel channel, IProgressMonitor monitor);

	/**
	 * Shutdown the communication to the given peer, no matter of the current
	 * reference count. A possible associated value-add is shutdown as well.
	 * <p>
	 * <b>Note:</b> Shutting down a channel is an asynchronous operation and the
	 * {@link #shutdown(IPeer)} method can return while shutting down the channel
	 * is still in progress.
	 * <p>
	 * The method can be called from any thread context.
	 *
	 * @param peer The peer. Must not be <code>null</code>.
	 */
	public void shutdown(IPeer peer);

	/**
	 * Shutdown the communication to the given peer, no matter of the current
	 * reference count. A possible associated value-add is shutdown as well.
	 *
	 * @param peer The peer. Must not be <code>null</code>.
	 * @param wait If <code>true</code> the method will wait until all channels or closed. If <code>false</code>,
	 *             the method will return immediately.
	 */
	public void shutdown(IPeer peer, boolean wait);

	/**
	 * Close all open channel, no matter of the current reference count.
	 * <p>
	 * If <code>wait</code> equals <code>false</code>, the method can be called
	 * from any thread context. Otherwise it must be called from outside the
	 * TCF event dispatch thread.
	 *
	 * @param wait If <code>true</code> the method will wait until all channels or closed. If <code>false</code>,
	 *             the method will return immediately.
	 */
	public void closeAll(boolean wait);

	/**
	 * Channel manager specific interface to be implemented by streams listener proxies.
	 */
	interface IStreamsListenerProxy {

		/**
		 * Trigger the processing of all delayed created events.
		 */
		void processDelayedCreatedEvents();
	}

    /**
     * Channel manager specific extension of the {@link IStreams.StreamsListener} interface
     * to handle the stream disconnect in a common place.
     *
     * @see IStreams.StreamsListener
     */
    interface IStreamsListener extends IStreams.StreamsListener {

    	/**
    	 * Associate the given proxy with the streams listener. The
    	 * streams listener can call the proxy methods to tell the
    	 * proxy implementation which created stream should be disconnected.
    	 *
    	 * @param proxy The streams listener proxy or <code>null</code>.
    	 */
    	void setProxy(IStreamsListenerProxy proxy);

    	/**
    	 * Returns if or if not the listener has a context set and can
    	 * decide if a created event is consumed or not.
    	 *
    	 * @return <code>True</code> if the listener has a context, <code>false</code> if not.
    	 */
    	boolean hasContext();

    	/**
    	 * Returns if or if not the given created event is consumed by the streams listener
    	 * or not.
    	 *
    	 * @param stream_type The stream type. Must not be <code>null</code>.
    	 * @param stream_id The stream id. Must not be <code>null</code>.
    	 * @param context_id The context id or <code>null</code>.
    	 *
    	 * @return <code>True</code> if the created event is consumed, <code>false</code> otherwise.
    	 */
    	boolean isCreatedConsumed(String stream_type, String stream_id, String context_id);
    }

	/**
	 * Client call back interface for subscribeStream(...).
	 */
	interface DoneSubscribeStream {
		/**
		 * Called when subscribing to a stream type is done.
		 *
		 * @param error The error description if operation failed, <code>null</code> if succeeded.
		 */
		void doneSubscribeStream(Throwable error);
	}

	/**
	 * Subscribe to the given stream type if not yet subscribed and register the given streams listener.
	 *
	 * @param channel The channel. Must not be <code>null</code>.
	 * @param streamType The stream source type. Must not be <code>null</code>.
	 * @param listener The streams listener. Must not be <code>null</code>.
	 * @param done The client callback. Must not be <code>null</code>.
	 */
	public void subscribeStream(IChannel channel, String streamType, IStreamsListener listener, DoneSubscribeStream done);

	/**
	 * Client call back interface for unsubscribeStream(...).
	 */
	interface DoneUnsubscribeStream {
		/**
		 * Called when unsubscribing a stream type is done.
		 *
		 * @param error The error description if operation failed, <code>null</code> if succeeded.
		 */
		void doneUnsubscribeStream(Throwable error);
	}

	/**
	 * Unsubscribe from the given stream type if subscribed and unregister the given streams listener.
	 *
	 * @param channel The channel. Must not be <code>null</code>.
	 * @param streamType The stream source type. Must not be <code>null</code>.
	 * @param listener The streams listener. Must not be <code>null</code>.
	 * @param done The client callback. Must not be <code>null</code>.
	 */
	public void unsubscribeStream(IChannel channel, String streamType, IStreamsListener listener, DoneUnsubscribeStream done);
}
