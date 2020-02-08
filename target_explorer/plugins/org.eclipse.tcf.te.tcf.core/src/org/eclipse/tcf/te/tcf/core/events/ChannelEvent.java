/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.events;

import java.util.EventObject;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;

/**
 * Channel event implementation.
 */
public final class ChannelEvent extends EventObject {
    private static final long serialVersionUID = 864759021559875199L;

    // Event type constants

    public static final String TYPE_OPENING = "opening"; //$NON-NLS-1$
	public static final String TYPE_REDIRECT = "redirect"; //$NON-NLS-1$
    public static final String TYPE_OPEN = "open"; //$NON-NLS-1$
	public static final String TYPE_CLOSE = "close"; //$NON-NLS-1$
	public static final String TYPE_MARK = "mark"; //$NON-NLS-1$
	public static final String TYPE_CLOSE_WRITER = "closeWriter"; //$NON-NLS-1$
	public static final String TYPE_SERVICS = "services"; //$NON-NLS-1$

	// Property constants
	public static final String PROP_MESSAGE = "message"; //$NON-NLS-1$
	public static final String PROP_LOG_NAME = "logname"; //$NON-NLS-1$

	// The channel
	private final IChannel channel;
	// The event type
	private final String type;
	// The event data
	private final IPropertiesContainer data;

	/**
	 * Constructor
	 *
	 * @param source The source object. Must not be <code>null</code>.
	 * @param channel The channel. Must not be <code>null</code>.
	 * @param type The event type. Must not be <code>null</code>.
	 * @param data The event data <code>null</code>.
	 */
	public ChannelEvent(Object source, IChannel channel, String type, IPropertiesContainer data) {
		super(source);

		if (channel == null) {
			throw new IllegalArgumentException("null channel"); //$NON-NLS-1$
		}

		if (type == null) {
			throw new IllegalArgumentException("null type"); //$NON-NLS-1$
		}

		this.channel = channel;
		this.type = type;
		this.data = data;
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
	 * Returns the event data.
	 *
	 * @return The event data or <code>null</code>.
	 */
	public IPropertiesContainer getData() {
		return data;
	}
}
