/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.internal.channelmanager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IStreams;
import org.eclipse.tcf.te.runtime.interfaces.IDisposable;
import org.eclipse.tcf.te.tcf.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.IStreamsListener;
import org.eclipse.tcf.te.tcf.core.interfaces.tracing.ITraceIds;

/**
 * Channel manager stream listener proxy implementation.
 */
final class StreamListenerProxy implements IStreams.StreamsListener, IChannelManager.IStreamsListenerProxy {
	// The channel
	private final IChannel channel;
	// The stream type the proxy is registered for
	private final String streamType;
	// The list of proxied stream listeners
	/* default */ ListenerList listeners = new ListenerList();
	// The list of delayed stream created events
	private final List<StreamListenerProxy.StreamCreatedEvent> delayedCreatedEvents = new ArrayList<StreamListenerProxy.StreamCreatedEvent>();

	/**
	 * Immutable stream created event.
	 */
	private final static class StreamCreatedEvent {
		/**
		 * The stream type.
		 */
		public final String streamType;
		/**
		 * The stream id.
		 */
		public final String streamId;
		/**
		 * The context id.
		 */
		public final String contextId;

		// As the class is immutable, we do not need to build the toString
		// value again and again. Build it once in the constructor and reuse it later.
		private final String toString;

		/**
		 * Constructor.
		 *
		 * @param streamType The stream type.
		 * @param streamId The stream id.
		 * @param contextId The context id.
		 */
		public StreamCreatedEvent(String streamType, String streamId, String contextId) {
			this.streamType = streamType;
			this.streamId = streamId;
			this.contextId = contextId;

			toString = toString();
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			return obj instanceof StreamListenerProxy.StreamCreatedEvent
					&& toString().equals(((StreamListenerProxy.StreamCreatedEvent)obj).toString());
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return toString().hashCode();
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			if (toString != null) return toString;

			StringBuilder builder = new StringBuilder(getClass().getSimpleName());
			builder.append(": streamType = "); //$NON-NLS-1$
			builder.append(streamType);
			builder.append("; streamId = "); //$NON-NLS-1$
			builder.append(streamId);
			builder.append("; contextId = "); //$NON-NLS-1$
			builder.append(contextId);

			return builder.toString();
		}
	}

	/**
     * Constructor
     *
     * @param The channel. Must not be <code>null</code>.
     */
    public StreamListenerProxy(final IChannel channel, final String streamType) {
    	Assert.isNotNull(channel);
    	Assert.isNotNull(streamType);

    	this.channel = channel;
    	this.channel.addChannelListener(new IChannel.IChannelListener() {
			@Override
			public void onChannelOpened() {}

			@Override
			public void onChannelClosed(Throwable error) {
				// Channel is closed, remove ourself
				channel.removeChannelListener(this);
				// Dispose all registered streams listener
				Object[] candidates = listeners.getListeners();
				listeners.clear();
				for (Object listener : candidates) {
					if (listener instanceof IDisposable) {
						((IDisposable)listener).dispose();
					}
				}
			}

			@Override
			public void congestionLevel(int level) {
			}
		});

    	// Remember the stream type
    	this.streamType = streamType;
    }

    /**
     * Returns the stream type the proxy is registered for.
     *
     * @return The stream type.
     */
    public String getStreamType() {
    	return streamType;
    }

    /**
     * Adds the given streams listener to the list of proxied listeners.
     *
     * @param listener The streams listener. Must not be <code>null</code>.
     */
    public void addListener(IStreamsListener listener) {
    	Assert.isNotNull(listener);
    	listener.setProxy(this);
    	listeners.add(listener);
    }

    /**
     * Removes the given streams listener from the list of proxied listeners.
     *
     * @param listener The streams listener. Must not be <code>null</code>.
     */
    public void removeListener(IStreamsListener listener) {
    	Assert.isNotNull(listener);
    	listener.setProxy(null);
    	listeners.remove(listener);
    }

    /**
     * Returns if the proxied listeners list is empty or not.
     *
     * @return <code>True</code> if the list is empty, <code>false</code> otherwise.
     */
    public boolean isEmpty() {
    	return listeners.isEmpty();
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.IStreamsListenerProxy#processDelayedCreatedEvents()
     */
    @Override
    public void processDelayedCreatedEvents() {
		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_STREAMS_LISTENER_PROXY)) {
			CoreBundleActivator.getTraceHandler().trace("StreamListenerProxy: processDelayedCreatedEvents()", //$NON-NLS-1$
			                                            0, ITraceIds.TRACE_STREAMS_LISTENER_PROXY,
			                                            IStatus.INFO, getClass());
		}

		synchronized (delayedCreatedEvents) {
			// Make a snapshot of all delayed created events
			StreamListenerProxy.StreamCreatedEvent[] events = delayedCreatedEvents.toArray(new StreamListenerProxy.StreamCreatedEvent[delayedCreatedEvents.size()]);
			// Clear the events now, it will be refilled by calling the created method
			delayedCreatedEvents.clear();
			// Loop the delayed created events and recall the created method to process them
			for (StreamListenerProxy.StreamCreatedEvent event : events) {
				created(event.streamType, event.streamId, event.contextId);
			}
		}
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IStreams.StreamsListener#created(java.lang.String, java.lang.String, java.lang.String)
	 */
    @Override
    public void created(String stream_type, String stream_id, String context_id) {
    	Assert.isNotNull(stream_type);
    	Assert.isNotNull(stream_id);

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_STREAMS_LISTENER_PROXY)) {
			CoreBundleActivator.getTraceHandler().trace("StreamListenerProxy: created(" + stream_type + ", " + stream_id + ", " + context_id + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			                                            0, ITraceIds.TRACE_STREAMS_LISTENER_PROXY,
			                                            IStatus.INFO, getClass());
		}

		// If the context_id is null, disconnect from the stream right away. We do not support
		// old TCF agents not sending the context id in the created event.
		if (context_id == null) {
			IStreams service = channel.getRemoteService(IStreams.class);
			if (service != null) {
				service.disconnect(stream_id, new IStreams.DoneDisconnect() {
					@Override
					public void doneDisconnect(IToken token, Exception error) {
						if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_STREAMS_LISTENER_PROXY)) {
							CoreBundleActivator.getTraceHandler().trace("StreamListenerProxy: disconnect -> context id must be not null.", //$NON-NLS-1$
																		0, ITraceIds.TRACE_STREAMS_LISTENER_PROXY, IStatus.INFO, getClass());
						}
					}
				});
			}

			return;
		}

		boolean delayed = false;
		boolean disconnect = true;

		// Loop all listeners
    	for (Object l : listeners.getListeners()) {
    		IStreamsListener listener = (IStreamsListener)l;

    		// If the listener has no context set yet, the listener cannot decide if
    		// the event is consumed or not. In this case, the event must be delayed.
    		if (!listener.hasContext()) {
    			delayed |= true;
    			continue;
    		}

    		// Does the listener consume the event?
    		boolean consume = listener.isCreatedConsumed(stream_type, stream_id, context_id);
    		if (consume) listener.created(stream_type, stream_id, context_id);
    		// If the created event is consumed by one listener, it cannot be disconnected anymore
    		disconnect &= !consume;
    	}

    	if (delayed) {
			// Context not set yet --> add to the delayed list
			StreamListenerProxy.StreamCreatedEvent event = new StreamCreatedEvent(stream_type, stream_id, context_id);
			synchronized (delayedCreatedEvents) {
				if (!delayedCreatedEvents.contains(event)) {
					delayedCreatedEvents.add(event);

					if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_STREAMS_LISTENER_PROXY)) {
						CoreBundleActivator.getTraceHandler().trace("StreamListenerProxy: delayed -> at least one listener does not have a context set", //$NON-NLS-1$
																	0, ITraceIds.TRACE_STREAMS_LISTENER_PROXY, IStatus.INFO, getClass());
					}
				}
			}
			return;
    	}

    	if (disconnect) {
			IStreams service = channel.getRemoteService(IStreams.class);
			if (service != null) {
				service.disconnect(stream_id, new IStreams.DoneDisconnect() {
					@Override
					public void doneDisconnect(IToken token, Exception error) {
						if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_STREAMS_LISTENER_PROXY)) {
							CoreBundleActivator.getTraceHandler().trace("StreamListenerProxy: disconnect -> not interested in context id", //$NON-NLS-1$
																		0, ITraceIds.TRACE_STREAMS_LISTENER_PROXY, IStatus.INFO, getClass());
						}
					}
				});
			}
    	}
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.IStreams.StreamsListener#disposed(java.lang.String, java.lang.String)
	 */
    @Override
    public void disposed(String stream_type, String stream_id) {
    	Assert.isNotNull(stream_type);
    	Assert.isNotNull(stream_id);

    	if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITraceIds.TRACE_STREAMS_LISTENER_PROXY)) {
			CoreBundleActivator.getTraceHandler().trace("StreamListenerProxy: disposed(" + stream_type + ", " + stream_id + ")", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			                                            0, ITraceIds.TRACE_STREAMS_LISTENER_PROXY,
			                                            IStatus.INFO, getClass());
		}

		// If the delayed created events list is not empty, we have
		// to check if one of the delayed create events got disposed
		synchronized (delayedCreatedEvents) {
			Iterator<StreamListenerProxy.StreamCreatedEvent> iterator = delayedCreatedEvents.iterator();
			while (iterator.hasNext()) {
				StreamListenerProxy.StreamCreatedEvent event = iterator.next();
				if (stream_type.equals(event.streamType) && stream_id.equals(event.streamId)) {
					// Remove the create event from the list
					iterator.remove();
				}
			}
		}

    	for (Object l : listeners.getListeners()) {
    		((IStreamsListener)l).disposed(stream_type, stream_id);
    	}
    }
}