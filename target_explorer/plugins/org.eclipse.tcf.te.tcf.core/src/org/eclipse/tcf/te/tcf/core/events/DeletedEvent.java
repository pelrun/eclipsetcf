/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.events;

import java.util.EventObject;

import org.eclipse.core.runtime.Assert;

/**
 * Peer node deleted notification event.
 */
public class DeletedEvent extends EventObject {
    private static final long serialVersionUID = 109800157682115887L;
	private final Object node;

	/**
	 * Constructor
	 *
	 * @param source The event source. Must not be <code>null</code>.
	 * @param commandId The command id. Must not be <code>null</code>.
	 */
	public DeletedEvent(Object source, Object node) {
		super(source);

		Assert.isNotNull(node);
		this.node = node;
	}

	/**
	 * Returns the peer node.
	 *
	 * @return The peer node.
	 */
	public final Object getPeerNode() {
		return node;
	}
}