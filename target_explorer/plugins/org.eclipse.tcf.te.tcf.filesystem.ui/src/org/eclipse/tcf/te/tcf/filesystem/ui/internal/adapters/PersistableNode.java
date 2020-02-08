/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.adapters;

import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

/**
 * The adapter class of IFSTreeNode for IPersistableElement, used to
 * persist an IFSTreeNode.
 */
public class PersistableNode implements IPersistableElement {
	// The node to be persisted.
	private IFSTreeNode node;
	/**
	 * Create an instance.
	 *
	 * @param node The node to be persisted.
	 */
	public PersistableNode(IFSTreeNode node) {
		this.node = node;
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IPersistable#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento memento) {
		memento.putString("peerId", node.getPeerNode().getPeerId()); //$NON-NLS-1$
		String path = null;
		if (!node.isFileSystem()) path = node.getLocation();
		if (path != null) memento.putString("path", path); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IPersistableElement#getFactoryId()
	 */
	@Override
	public String getFactoryId() {
		return "org.eclipse.tcf.te.tcf.filesystem.ui.nodeFactory"; //$NON-NLS-1$
	}
}
