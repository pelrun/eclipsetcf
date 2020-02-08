/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal.adapters;

import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

/**
 * The persistable element implementation for an IPeerNode.
 */
public class PersistablePeerNode implements IPersistableElement {
	// The peer model to be persisted.
	private IPeerNode peerNode;

	/**
	 * Constructor
	 */
	public PersistablePeerNode(IPeerNode peerNode) {
		this.peerNode = peerNode;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IPersistable#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento memento) {
		memento.putString("peerId", peerNode.getPeerId()); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IPersistableElement#getFactoryId()
	 */
	@Override
	public String getFactoryId() {
		return "org.eclipse.tcf.te.ui.views.peerFactory"; //$NON-NLS-1$
	}
}
