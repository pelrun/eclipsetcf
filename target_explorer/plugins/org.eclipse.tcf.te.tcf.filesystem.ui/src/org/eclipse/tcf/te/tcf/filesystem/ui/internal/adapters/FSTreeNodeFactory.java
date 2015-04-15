/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.adapters;

import static org.eclipse.tcf.te.tcf.locator.model.ModelManager.getPeerModel;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IResultOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.filesystem.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

/**
 * The element factory for IFSTreeNode used to restore FSTreeNodes persisted
 * for expanded states.
 */
public class FSTreeNodeFactory implements IElementFactory {
	@Override
	public IAdaptable createElement(IMemento memento) {
		String peerId = memento.getString("peerId"); //$NON-NLS-1$
		if (peerId == null)
			return null;

		for (IPeerNode peerNode : getPeerModel().getPeerNodes()) {
			if (peerNode.getPeerId().equals(peerId)) {
				IRuntimeModel rtm = ModelManager.getRuntimeModel(peerNode);
				if (rtm != null) {
					String path = memento.getString("path"); //$NON-NLS-1$
					if (path == null) {
						return rtm.getRoot();
					}
					IResultOperation<IFSTreeNode> op = rtm.operationRestoreFromPath(path);
					if (op.run(null).isOK()) {
						return op.getResult();
					}
				}
				return null;
			}
		}
		return null;
	}
}
