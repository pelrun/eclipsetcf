/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
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
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Peer model step context implementation.
 */
public class PeerNodeStepContext extends AbstractStepContext {

	/**
     * Constructor
     */
    public PeerNodeStepContext(IPeerNode peerNode) {
    	super(peerNode);
    }

	/**
	 * Returns the peer model.
	 * @return The peer model.
	 */
	public IPeerNode getPeerModel() {
		return (IPeerNode)getContextObject();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getId()
	 */
	@Override
	public String getId() {
		return getPeerModel().getPeerId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext#getName()
	 */
	@Override
	public String getName() {
		return getPeerModel().getName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(final Class adapter) {
		if (IPeerNode.class.equals(adapter)) {
			return getPeerModel();
		}

		if (IPeer.class.equals(adapter)) {
			return getPeerModel().getPeer();
		}

		return super.getAdapter(adapter);
	}
}
