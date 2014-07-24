/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.log.core.internal.listener;

import java.util.EventObject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventFireDelegate;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.tcf.core.events.ChannelEvent;

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
			final IChannel channel = ((ChannelEvent)event).getChannel();
			final String type = ((ChannelEvent)event).getType();
			final String message = ((ChannelEvent)event).getMessage();

			if (ChannelEvent.TYPE_OPENING.equals(type)) {
				ChannelTraceListenerManager.getInstance().onChannelOpening(channel, message);
			}
			else if (ChannelEvent.TYPE_REDIRECT.equals(type)) {
				ChannelTraceListenerManager.getInstance().onChannelRedirected(channel, message);
			}
			else if (ChannelEvent.TYPE_OPEN.equals(type)) {
				ChannelTraceListenerManager.getInstance().onChannelOpened(channel, message);
			}
			else if (ChannelEvent.TYPE_CLOSE.equals(type)) {
				ChannelTraceListenerManager.getInstance().onChannelClosed(channel);
			}
			else if (ChannelEvent.TYPE_MARK.equals(type)) {
				ChannelTraceListenerManager.getInstance().onMark(channel, message);
			}
		}
	}

}
