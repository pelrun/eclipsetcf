/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.internal.adapters;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.runtime.stepper.context.AbstractStepContext;

/**
 * Peer step context implementation.
 */
public class PeerStepContext extends AbstractStepContext {

	/**
     * Constructor
     */
    public PeerStepContext(IPeer peer) {
    	super(peer);
    }

	/**
	 * Returns the peer.
	 * @return The peer.
	 */
	public IPeer getPeer() {
		return (IPeer)getContextObject();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getId()
	 */
	@Override
	public String getId() {
		return getPeer().getID();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getName()
	 */
	@Override
	public String getName() {
		return getPeer().getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(final Class adapter) {
		if (IPeer.class.equals(adapter)) {
			return getPeer();
		}

		return super.getAdapter(adapter);
	}
}
