/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.adapters;

import org.eclipse.tcf.te.core.interfaces.IViewerInput;
import org.eclipse.tcf.te.core.utils.PropertyChangeProvider;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * The viewer input of an IPeerNode instance.
 */
public class PeerNodeViewerInput extends PropertyChangeProvider implements IViewerInput {
	// The peer model.
	private IPeerNode peerNode;
	
	/**
	 * Create an instance with a peer model.
	 * 
	 * @param peerNode The peer model.
	 */
	public PeerNodeViewerInput(IPeerNode peerNode) {
		this.peerNode = peerNode;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.IViewerInput#getInputId()
	 */
	@Override
    public String getInputId() {
	    return peerNode.getPeerId();
    }
}
