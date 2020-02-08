/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.interfaces.steps;

import org.eclipse.tcf.te.tcf.core.internal.channelmanager.iterators.ChainPeersIterator;


/**
 * Defines locator related step data attribute id's.
 */
public interface ITcfStepAttributes {

	/**
	 * Define the prefix used by all other attribute id's as prefix.
	 */
	public static final String ATTR_PREFIX = "org.eclipse.tcf.te.tcf.locator"; //$NON-NLS-1$

	/**
	 * Step attribute: The TCF channel.
	 */
	public static final String ATTR_CHANNEL = ITcfStepAttributes.ATTR_PREFIX + ".channel"; //$NON-NLS-1$

	/**
	 * Step attribute: The token for a running TCF command.
	 */
	public static final String ATTR_RUNNING_TOKEN = ITcfStepAttributes.ATTR_PREFIX + ".running_token"; //$NON-NLS-1$

	/**
	 * Step attribute: The value add.
	 */
	public static final String ATTR_VALUE_ADD = ITcfStepAttributes.ATTR_PREFIX + ".value_add"; //$NON-NLS-1$

	/**
	 * Step attribute: Flag if the peer should not be added to the list of peers to chain in {@link ChainPeersIterator}.
	 */
	public static final String ATTR_SKIP_PEER_TO_CHAIN = ITcfStepAttributes.ATTR_PREFIX + ".skip_peer_to_chain"; //$NON-NLS-1$

	/**
	 * Step attribute: The log file name
	 */
	public static final String ATTR_LOG_NAME = ITcfStepAttributes.ATTR_PREFIX + ".logname"; //$NON-NLS-1$
}
