/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.channelmanager;

import org.eclipse.core.runtime.Assert;

/**
 * Special exception thrown if <code>peer.openChannel()</code> fails. The exception
 * will wrap the original error but allows handlers to distinguish between errors happening
 * while trying to open the channel and errors happening after the <code>peer.openChannel()</code>,
 * thrown by failing steps of the open channel step group.
 */
public class OpenChannelException extends Throwable {
    private static final long serialVersionUID = 4715084774433865088L;
	private final Throwable error;

	/**
	 * Constructor.
	 *
	 * @param error The error. Must not be <code>null</code>.
	 */
	public OpenChannelException(Throwable error) {
		super();
		Assert.isNotNull(error);
		this.error = error;
	}

	/**
	 * Returns the error.
	 *
	 * @return The error.
	 */
	public Throwable getError() {
		return error;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof OpenChannelException) {
			return error.equals(((OpenChannelException)obj).error);
		}
	    return super.equals(obj);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
	    return error.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#toString()
	 */
    @Override
    public String toString() {
	    return error.toString();
    }
}
