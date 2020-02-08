/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.log.core.internal.listener;

import java.util.EventObject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventFireDelegate;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.tcf.core.events.ChannelEvent;
import org.eclipse.tcf.te.tcf.log.core.manager.LogManager;

/**
 * Channel event listener.
 */
public final class EventListener extends PlatformObject implements IEventListener, IEventFireDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventFireDelegate#fire(java.lang.Runnable)
	 */
	@Override
	public void fire(Runnable runnable) {
		if (Protocol.isDispatchThread()) runnable.run();
		else Protocol.invokeLater(runnable);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventListener#eventFired(java.util.EventObject)
	 */
	@Override
	public void eventFired(EventObject event) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (event instanceof ChannelEvent) {
			IChannel channel = ((ChannelEvent)event).getChannel();
			String type = ((ChannelEvent)event).getType();
			IPropertiesContainer data = ((ChannelEvent)event).getData();

			final String message = data != null ? data.getStringProperty(ChannelEvent.PROP_MESSAGE) : null;
			String logname = data != null ? data.getStringProperty(ChannelEvent.PROP_LOG_NAME) : null;

			if (logname != null) logname = LogManager.getInstance().makeValid(logname);

			if (ChannelEvent.TYPE_OPENING.equals(type)) {
				ChannelTraceListenerManager.getInstance().onChannelOpening(logname, channel, message);
			}
			else if (ChannelEvent.TYPE_REDIRECT.equals(type)) {
				ChannelTraceListenerManager.getInstance().onChannelRedirected(logname, channel, message);
			}
			else if (ChannelEvent.TYPE_OPEN.equals(type)) {
				ChannelTraceListenerManager.getInstance().onChannelOpened(logname, channel, message);
			}
			else if (ChannelEvent.TYPE_CLOSE.equals(type)) {
				ChannelTraceListenerManager.getInstance().onChannelClosed(logname, channel);
			}
			else if (ChannelEvent.TYPE_MARK.equals(type)) {
				ChannelTraceListenerManager.getInstance().onMark(logname, channel, message);
			}
			else if (ChannelEvent.TYPE_CLOSE_WRITER.equals(type)) {
				// Determine the remote peer from the channel
				final IPeer peer = channel.getRemotePeer();
				if (peer != null) {
					final String lognameFinal = logname;

					// This method is called in the TCF event dispatch thread. There
					// is no need that the logging itself keeps the TCF event dispatch
					// thread busy. Execute the logging itself in a separate thread but
					// still maintain the order of the messages.
					ExecutorsUtil.execute(new Runnable() {
						@Override
						public void run() {
							LogManager.getInstance().closeWriter(lognameFinal, peer, message);
						}
					});
				}
			}
			else if (ChannelEvent.TYPE_SERVICS.equals(type)) {
				ChannelTraceListenerManager.getInstance().onChannelServices(logname, channel, message);
			}
		}
	}

}
