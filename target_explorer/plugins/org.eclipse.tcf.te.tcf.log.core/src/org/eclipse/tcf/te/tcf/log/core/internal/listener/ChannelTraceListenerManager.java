/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.log.core.internal.listener;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.core.AbstractChannel;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.tcf.log.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.log.core.events.MonitorEvent;
import org.eclipse.tcf.te.tcf.log.core.interfaces.IPreferenceKeys;
import org.eclipse.tcf.te.tcf.log.core.interfaces.ITracing;
import org.eclipse.tcf.te.tcf.log.core.internal.nls.Messages;
import org.eclipse.tcf.te.tcf.log.core.manager.LogManager;

/**
 * TCF logging channel trace listener manager implementation.
 */
public final class ChannelTraceListenerManager {
	/**
	 * Time format representing date and time with milliseconds.
	 */
	public final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); //$NON-NLS-1$

	// The map of trace listeners per channel
	private final Map<IChannel, AbstractChannel.TraceListener> listeners = new HashMap<IChannel, AbstractChannel.TraceListener>();

	// The map of queued messaged per channel
	/* default */ final Map<IChannel, List<String>> queued = new HashMap<IChannel, List<String>>();

	/*
	 * Thread save singleton instance creation.
	 */
	private static class LazyInstanceHolder {
		public static ChannelTraceListenerManager instance = new ChannelTraceListenerManager();
	}

	/**
	 * Returns the singleton instance for the manager.
	 */
	public static ChannelTraceListenerManager getInstance() {
		return LazyInstanceHolder.instance;
	}

	/**
	 * Constructor.
	 */
	/* default */ ChannelTraceListenerManager() {
	}

	/**
	 * New channel opened. Attach a channel trace listener.
	 *
	 * @param logname The log name or <code>null</code>.
	 * @param channel The channel. Must not be <code>null</code>.
	 * @param message A message or <code>null</code>.
	 */
	public void onChannelOpened(final String logname, final IChannel channel, final String message) {
		Assert.isNotNull(channel);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// The trace listener interface does not have a onChannelOpenend method, but
		// for consistency, log the channel opening similar to the others.
		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("TraceListener.onChannelOpened ( " + channel + ", \"" + message + "\" )", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
														ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER, this);
		}

		// The trace listeners can be accessed only via AbstractChannel
		if (!(channel instanceof AbstractChannel)) return;

		// Determine the remote peer from the channel
		final IPeer peer = channel.getRemotePeer();
		if (peer == null) return;

		// Get the preference key if or if not logging is enabled
		boolean loggingEnabled = CoreBundleActivator.getScopedPreferences().getBoolean(IPreferenceKeys.PREF_LOGGING_ENABLED);
		// If false, we are done here and wont create any console or trace listener.
		if (!loggingEnabled) return;

		// As the channel has just opened, there should be no trace listener, but better be safe and check
		AbstractChannel.TraceListener traceListener = listeners.remove(channel);
		if (traceListener != null) ((AbstractChannel)channel).removeTraceListener(traceListener);
		// Create a new trace listener instance
		traceListener = new ChannelTraceListener(logname, channel);
		// Attach trace listener to the channel
		((AbstractChannel)channel).addTraceListener(traceListener);
		// Remember the associated trace listener
		listeners.put(channel, traceListener);

		// Determine the date and time of the message before spawning to the log thread.
		final String date = DATE_FORMAT.format(new Date(System.currentTimeMillis()));

		// This method is called in the TCF event dispatch thread. There
		// is no need that the logging itself keeps the TCF event dispatch
		// thread busy. Execute the logging itself in a separate thread but
		// still maintain the order of the messages.
		ExecutorsUtil.execute(new Runnable() {
			@Override
			public void run() {
				String fullMessage = NLS.bind(Messages.ChannelTraceListener_channelOpened_message,
												new Object[] {
													date,
													Integer.toHexString(channel.hashCode()),
													message != null ? "(" + message.trim() + ")" : "" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
										  		});

				// Get the file writer
				FileWriter writer = LogManager.getInstance().getWriter(logname, peer);
				if (writer != null) {
					try {
						writer.write("\n\n\n"); //$NON-NLS-1$

						// Get the queued messages
						List<String> queue = queued.remove(channel);

						// Write the queued messages
						if (queue != null) {
							for (String m : queue) {
								writer.write(m);
								writer.write("\n"); //$NON-NLS-1$
							}
						}

						// Write the opened message
						writer.write(fullMessage);
						writer.write("\n"); //$NON-NLS-1$
						writer.flush();
					} catch (IOException e) {
						/* ignored on purpose */
					}
				}
				LogManager.getInstance().monitor(peer, MonitorEvent.Type.OPEN, new MonitorEvent.Message('F', fullMessage));
			}
		});

	}

	/**
	 * Channel is opening.
	 * <p>
	 * This is the state where {@link IPeer#openChannel()} got called but no
	 * further redirect or channel listener got invoked.
	 *
	 * @param logname The log name or <code>null</code>.
	 * @param channel The channel. Must not be <code>null</code>.
	 * @param message A message or <code>null</code>.
	 */
	public void onChannelOpening(final String logname, final IChannel channel, final String message) {
		Assert.isNotNull(channel);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("TraceListener.onChannelOpening ( " + channel + ", \"" + message + "\" )", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
														ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER, this);
		}

		// Get the preference key if or if not logging is enabled
		boolean loggingEnabled = CoreBundleActivator.getScopedPreferences().getBoolean(IPreferenceKeys.PREF_LOGGING_ENABLED);
		// If false, we are done here and wont create any console or trace listener.
		if (!loggingEnabled) return;

		// Determine the date and time of the message before spawning to the log thread.
		final String date = DATE_FORMAT.format(new Date(System.currentTimeMillis()));

		// This method is called in the TCF event dispatch thread. There
		// is no need that the logging itself keeps the TCF event dispatch
		// thread busy. Execute the logging itself in a separate thread but
		// still maintain the order of the messages.
		ExecutorsUtil.execute(new Runnable() {
			@Override
			public void run() {
				String fullMessage = NLS.bind(Messages.ChannelTraceListener_channelOpening_message,
											  new Object[] {
												date,
												Integer.toHexString(channel.hashCode()),
												message != null ? "(" + message.trim() + ")" : "" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
											  });

				List<String> queue = queued.get(channel);
				if (queue == null) {
					queue = new ArrayList<String>();
					queued.put(channel, queue);
				}
				queue.add(fullMessage);
			}
		});
	}

	/**
	 * Channel got redirected.
	 *
	 * @param logname The log name or <code>null</code>.
	 * @param channel The channel. Must not be <code>null</code>.
	 * @param message A message or <code>null</code>.
	 */
	public void onChannelRedirected(final String logname, final IChannel channel, final String message) {
		Assert.isNotNull(channel);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("TraceListener.onChannelRedirected ( " + channel + ", \"" + message + "\" )", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
														ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER, this);
		}

		// Get the preference key if or if not logging is enabled
		boolean loggingEnabled = CoreBundleActivator.getScopedPreferences().getBoolean(IPreferenceKeys.PREF_LOGGING_ENABLED);
		// If false, we are done here and wont create any console or trace listener.
		if (!loggingEnabled) return;

		// Determine the date and time of the message before spawning to the log thread.
		final String date = DATE_FORMAT.format(new Date(System.currentTimeMillis()));

		// This method is called in the TCF event dispatch thread. There
		// is no need that the logging itself keeps the TCF event dispatch
		// thread busy. Execute the logging itself in a separate thread but
		// still maintain the order of the messages.
		ExecutorsUtil.execute(new Runnable() {
			@Override
			public void run() {
				String fullMessage = NLS.bind(Messages.ChannelTraceListener_channelRedirected_message,
											  new Object[] {
												date,
												Integer.toHexString(channel.hashCode()),
												message != null ? "(" + message.trim() + ")" : "" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
											  });

				List<String> queue = queued.get(channel);
				if (queue == null) {
					queue = new ArrayList<String>();
					queued.put(channel, queue);
				}
				queue.add(fullMessage);
			}
		});
	}

	/**
	 * Channel remote services got queried.
	 *
	 * @param logname The log name or <code>null</code>.
	 * @param channel The channel. Must not be <code>null</code>.
	 * @param message A message or <code>null</code>.
	 */
	public void onChannelServices(final String logname, final IChannel channel, final String message) {
		Assert.isNotNull(channel);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("TraceListener.onChannelServices ( " + channel + ", \"" + message + "\" )", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
														ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER, this);
		}

		// Get the preference key if or if not logging is enabled
		boolean loggingEnabled = CoreBundleActivator.getScopedPreferences().getBoolean(IPreferenceKeys.PREF_LOGGING_ENABLED);
		// If false, we are done here and wont create any console or trace listener.
		if (!loggingEnabled) return;

		// Determine the date and time of the message before spawning to the log thread.
		final String date = DATE_FORMAT.format(new Date(System.currentTimeMillis()));

		// This method is called in the TCF event dispatch thread. There
		// is no need that the logging itself keeps the TCF event dispatch
		// thread busy. Execute the logging itself in a separate thread but
		// still maintain the order of the messages.
		ExecutorsUtil.execute(new Runnable() {
			@Override
			public void run() {
				String fullMessage = NLS.bind(Messages.ChannelTraceListener_channelServices_message,
											  new Object[] {
												date,
												Integer.toHexString(channel.hashCode()),
												message != null ? "(" + message.trim() + ")" : "" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
											  });

				List<String> queue = queued.get(channel);
				if (queue == null) {
					queue = new ArrayList<String>();
					queued.put(channel, queue);
				}
				queue.add(fullMessage);
			}
		});
	}

	/**
	 * Mark an event in the channel log.
	 *
	 * @param logname The log name or <code>null</code>.
	 * @param channel The channel. Must not be <code>null</code>.
	 * @param message A message or <code>null</code>.
	 */
	public void onMark(final String logname, final IChannel channel, final String message) {
		Assert.isNotNull(channel);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// The trace listener interface does not have a onChannelOpenend method, but
		// for consistency, log the channel opening similar to the others.
		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("TraceListener.onMark ( " + channel + ", \"" + message + "\" )", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
														ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER, this);
		}

		// Determine the remote peer from the channel
		final IPeer peer = channel.getRemotePeer();
		if (peer == null) return;

		// Get the preference key if or if not logging is enabled
		boolean loggingEnabled = CoreBundleActivator.getScopedPreferences().getBoolean(IPreferenceKeys.PREF_LOGGING_ENABLED);
		// If false, we are done here and wont create any console or trace listener.
		if (!loggingEnabled) return;

		// Determine the date and time of the message before spawning to the log thread.
		final String date = DATE_FORMAT.format(new Date(System.currentTimeMillis()));

		// This method is called in the TCF event dispatch thread. There
		// is no need that the logging itself keeps the TCF event dispatch
		// thread busy. Execute the logging itself in a separate thread but
		// still maintain the order of the messages.
		ExecutorsUtil.execute(new Runnable() {
			@Override
			public void run() {
				String fullMessage = NLS.bind(Messages.ChannelTraceListener_channelMark_message,
												new Object[] {
													date,
													Integer.toHexString(channel.hashCode()),
													message != null ? "(" + message.trim() + ")" : "" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
										  		});

				// Get the file writer
				FileWriter writer = LogManager.getInstance().getWriter(logname, peer);
				if (writer != null) {
					try {
						// Write the message
						writer.write(fullMessage);
						writer.write("\n"); //$NON-NLS-1$
						writer.flush();
					} catch (IOException e) {
						/* ignored on purpose */
					}
				}
			}
		});
	}

	/**
	 * Channel closed. Detach the channel trace listener if any.
	 *
	 * @param logname The log name or <code>null</code>.
	 * @param channel The channel. Must not be <code>null</code>.
	 */
	public void onChannelClosed(String logname, final IChannel channel) {
		Assert.isNotNull(channel);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		ExecutorsUtil.execute(new Runnable() {
			@Override
			public void run() {
				// Remove the queued messages
				queued.remove(channel);
			}
		});

		// The trace listeners can be accessed only via AbstractChannel
		if (!(channel instanceof AbstractChannel)) return;

		// Remove the trace listener if any
		final AbstractChannel.TraceListener traceListener = listeners.remove(channel);
		if (traceListener != null) {
			// Removal needs to happen asynchronous is another dispatch cycle,
			// otherwise the closed event is not logged.
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					((AbstractChannel)channel).removeTraceListener(traceListener);
				}
			});
		}
	}
}
