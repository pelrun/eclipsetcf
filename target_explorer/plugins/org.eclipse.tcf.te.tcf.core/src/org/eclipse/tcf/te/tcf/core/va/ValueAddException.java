/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.va;

import org.eclipse.core.runtime.Assert;

/**
 * Value add exception implementation.
 */
public final class ValueAddException extends Throwable {
    private static final long serialVersionUID = -6926835359784354123L;
	private final Exception error;

	/**
	 * Constructor.
	 *
	 * @param error The error. Must not be <code>null</code>.
	 */
	public ValueAddException(Exception error) {
		super();
		Assert.isNotNull(error);
		this.error = error;
	}

	/**
	 * Returns the error.
	 *
	 * @return The error.
	 */
	public Exception getError() {
		return error;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ValueAddException) {
			return error.equals(((ValueAddException)obj).error);
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
