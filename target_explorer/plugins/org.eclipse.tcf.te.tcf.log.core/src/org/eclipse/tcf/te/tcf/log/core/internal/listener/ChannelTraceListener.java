/*******************************************************************************
 * Copyright (c) 2011 - 2015 Wind River Systems, Inc. and others. All rights reserved.
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
import java.util.Date;

import org.eclipse.core.runtime.Assert;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.core.AbstractChannel.TraceListener;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IDiagnostics;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.tcf.core.util.JSONUtils;
import org.eclipse.tcf.te.tcf.log.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.log.core.events.MonitorEvent;
import org.eclipse.tcf.te.tcf.log.core.interfaces.IPreferenceKeys;
import org.eclipse.tcf.te.tcf.log.core.interfaces.ITracing;
import org.eclipse.tcf.te.tcf.log.core.internal.nls.Messages;
import org.eclipse.tcf.te.tcf.log.core.manager.LogManager;

/**
 * TCF logging channel trace listener implementation.
 */
public final class ChannelTraceListener implements TraceListener {
	/**
	 * Time format representing time with milliseconds.
	 */
	public final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS"); //$NON-NLS-1$

	/**
	 * Time format representing date and time with milliseconds.
	 */
	public final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); //$NON-NLS-1$

	// Reference to the channel
	/* default */ final IChannel channel;
	// The log name
	/* default */ final String logname;

	/* default */ final boolean reverseReceived;

	/**
	 * Constructor.
	 *
	 * @param logname The log name or <code>null</code>.
	 * @param channel The channel. Must not be <code>null</code>.
	 */
	public ChannelTraceListener(String logname, IChannel channel) {
		this.logname = logname;

		Assert.isNotNull(channel);
		this.channel = channel;

		reverseReceived = channel.getRemotePeer().getName() != null && channel.getRemotePeer().getName().endsWith("Command Server"); //$NON-NLS-1$
	}

	/**
	 * Return the associated channel.
	 *
	 * @return The channel instance.
	 */
	protected final IChannel getChannel() {
		return channel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.core.AbstractChannel.TraceListener#onChannelClosed(java.lang.Throwable)
	 */
	@Override
	public void onChannelClosed(final Throwable error) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("TraceListener.onChannelClosed ( " + error + " )", //$NON-NLS-1$ //$NON-NLS-2$
														ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER, this);
		}

		// Determine the remote peer from the channel
		final IPeer peer = channel.getRemotePeer();
		if (peer == null) return;

		// Determine the date and time of the message before spawning to the log thread.
		final String date = DATE_FORMAT.format(new Date(System.currentTimeMillis()));

		// This method is called in the TCF event dispatch thread. There
		// is no need that the logging itself keeps the TCF event dispatch
		// thread busy. Execute the logging itself in a separate thread but
		// still maintain the order of the messages.
		ExecutorsUtil.execute(new Runnable() {
			@Override
			public void run() {
				final String message = NLS.bind(Messages.ChannelTraceListener_channelClosed_message,
												new Object[] {
													date,
													Integer.toHexString(channel.hashCode()),
													error
												});

				// Get the file writer
				FileWriter writer = LogManager.getInstance().getWriter(logname, peer);
				if (writer != null) {
					try {
						writer.write(message);
						writer.write("\n"); //$NON-NLS-1$
						writer.flush();
					} catch (IOException e) {
						/* ignored on purpose */
					}
				}

				LogManager.getInstance().monitor(peer, MonitorEvent.Type.CLOSE, new MonitorEvent.Message('F', message));
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.core.AbstractChannel.TraceListener#onMessageReceived(char, java.lang.String, java.lang.String, java.lang.String, byte[])
	 */
	@Override
	public void onMessageReceived(final char type, final String token, final String service, final String name, final byte[] data) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("TraceListener.onMessageReceived ( " + type //$NON-NLS-1$
														+ ", " + token + ", " + service + ", " + name + ", ... )", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
														ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER, this);
		}

		// Determine the remote peer from the channel
		final IPeer peer = channel.getRemotePeer();
		if (peer == null) return;

		// This method is called in the TCF event dispatch thread. There
		// is no need that the logging itself keeps the TCF event dispatch
		// thread busy. Execute the logging itself in a separate thread but
		// still maintain the order of the messages.
		ExecutorsUtil.execute(new Runnable() {
			@Override
			public void run() {
				doLogMessage(peer, type, token, service, name, data, reverseReceived ? false : true);
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.core.AbstractChannel.TraceListener#onMessageSent(char, java.lang.String, java.lang.String, java.lang.String, byte[])
	 */
	@Override
	public void onMessageSent(final char type, final String token, final String service, final String name, final byte[] data) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("TraceListener.onMessageSent ( " + type //$NON-NLS-1$
														+ ", " + token + ", " + service + ", " + name + ", ... )", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
														ITracing.ID_TRACE_CHANNEL_TRACE_LISTENER, this);
		}

		// Determine the remote peer from the channel
		final IPeer peer = channel.getRemotePeer();
		if (peer == null) return;

		// This method is called in the TCF event dispatch thread. There
		// is no need that the logging itself keeps the TCF event dispatch
		// thread busy. Execute the logging itself in a separate thread but
		// still maintain the order of the messages.
		ExecutorsUtil.execute(new Runnable() {
			@Override
			public void run() {
				doLogMessage(peer, type, token, service, name, data, reverseReceived ? true : false);
			}
		});
	}

	/**
	 * Helper method to output the message to the logger.
	 */
	/* default */ void doLogMessage(final IPeer peer, final char type, String token, String service, String name, byte[] data, boolean received) {
		Assert.isNotNull(peer);
		Assert.isTrue(ExecutorsUtil.isExecutorThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// Filter out the locator service messages
		boolean locatorEvents =  CoreBundleActivator.getScopedPreferences().getBoolean(IPreferenceKeys.PREF_SHOW_LOCATOR_EVENTS);
		if (!locatorEvents && service != null && service.toLowerCase().equals("locator")) { //$NON-NLS-1$
			return;
		}
		// Filter out the heart beat messages if not overwritten by the preferences
		boolean showHeartbeats = CoreBundleActivator.getScopedPreferences().getBoolean(IPreferenceKeys.PREF_SHOW_HEARTBEATS);
		if (!showHeartbeats && name != null && name.toLowerCase().contains("heartbeat")) { //$NON-NLS-1$
			return;
		}
		// Filter out framework events if not overwritten by the preferences
		boolean frameworkEvents = CoreBundleActivator.getScopedPreferences().getBoolean(IPreferenceKeys.PREF_SHOW_FRAMEWORK_EVENTS);
		if (!frameworkEvents && type == 'F') {
			return;
		}

		// Decode the arguments again for tracing purpose
		String args = JSONUtils.decodeStringFromByteArray(data);

		// Filter out 'Diagnostic echo "ping"' and response
		if ((type == 'C' && IDiagnostics.NAME.equals(service) && "echo".equals(name) && "\"ping\"".equals(args)) //$NON-NLS-1$ //$NON-NLS-2$
				|| (type == 'R' && service == null && name == null && "\"ping\"".equals(args))) { //$NON-NLS-1$
			return;
		}

		// Format the message
		final String message = formatMessage(type, token, service, name, args, received);
		// Get the file writer
		FileWriter writer = LogManager.getInstance().getWriter(logname, peer);
		if (writer != null) {
			try {
				writer.write(message);
				writer.write("\n"); //$NON-NLS-1$
				writer.flush();
			} catch (IOException e) {
				/* ignored on purpose */
			}
		}

		LogManager.getInstance().monitor(peer, MonitorEvent.Type.ACTIVITY, new MonitorEvent.Message(type, message));
	}

	/**
	 * Format the trace message.
	 */
	private String formatMessage(char type, String token, String service, String name, String args, boolean received) {
		// Get the current time stamp
		String time = TIME_FORMAT.format(new Date(System.currentTimeMillis()));

		// Construct the full message
		//
		// The message format is: <time>: [<---|--->] <type> <token> <service>#<name> <args>
		StringBuilder message = new StringBuilder();
		message.append(time).append(":"); //$NON-NLS-1$
		message.append(" [").append(Integer.toHexString(channel.hashCode())).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
		message.append(" ").append(received ? "<---" : "--->"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		message.append(" ").append(Character.valueOf(type)); //$NON-NLS-1$
		if (token != null) message.append(" ").append(token); //$NON-NLS-1$
		if (service != null) message.append(" ").append(service); //$NON-NLS-1$
		if (name != null) message.append(" ").append(name); //$NON-NLS-1$
		if (args != null && args.trim().length() > 0) message.append(" ").append(args.trim()); //$NON-NLS-1$

		return message.toString();
	}

}
