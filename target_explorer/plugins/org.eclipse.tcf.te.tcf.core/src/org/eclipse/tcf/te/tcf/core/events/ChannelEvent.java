/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.events;

import java.util.EventObject;

import org.eclipse.tcf.protocol.IChannel;

/**
 * Channel event implementation.
 */
public final class ChannelEvent extends EventObject {
    private static final long serialVersionUID = 864759021559875199L;

    public static final String TYPE_OPENING = "opening"; //$NON-NLS-1$
	public static final String TYPE_REDIRECT = "redirect"; //$NON-NLS-1$
    public static final String TYPE_OPEN = "open"; //$NON-NLS-1$
	public static final String TYPE_CLOSE = "close"; //$NON-NLS-1$
	public static final String TYPE_MARK = "mark"; //$NON-NLS-1$
	public static final String TYPE_CLOSE_WRITER = "closeWriter"; //$NON-NLS-1$

	// The channel
	private IChannel channel;
	// The event type
	private String type;
	// The optional message
	private String message;

	/**
	 * Constructor
	 *
	 * @param source The source object. Must not be <code>null</code>.
	 * @param channel The channel. Must not be <code>null</code>.
	 * @param type The event type. Must not be <code>null</code>.
	 * @param message A message or <code>null</code>.
	 */
	public ChannelEvent(Object source, IChannel channel, String type, String message) {
		super(source);

		if (channel == null) {
			throw new IllegalArgumentException("null channel"); //$NON-NLS-1$
		}

		if (type == null) {
			throw new IllegalArgumentException("null type"); //$NON-NLS-1$
		}

		this.channel = channel;
		this.type = type;
		this.message = message;
	}

	/**
	 * Returns the channel.
	 *
	 * @return The channel.
	 */
	public IChannel getChannel() {
		return channel;
	}

	/**
	 * Returns the event type.
	 *
	 * @return The event type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the optional message.
	 *
	 * @return The optional message or <code>null</code>.
	 */
	public String getMessage() {
		return message;
	}
}
