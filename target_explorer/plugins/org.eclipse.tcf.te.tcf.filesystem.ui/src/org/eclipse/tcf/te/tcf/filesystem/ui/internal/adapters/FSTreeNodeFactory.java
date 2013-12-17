/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.adapters;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.IOpExecutor;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.NullOpExecutor;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpParsePath;
import org.eclipse.tcf.te.tcf.filesystem.core.model.FSModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.model.Model;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

/**
 * The element factory for FSTreeNode used to restore FSTreeNodes persisted
 * for expanded states.
 */
public class FSTreeNodeFactory implements IElementFactory {
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
	 */
	@Override
	public IAdaptable createElement(IMemento memento) {
		String peerId = memento.getString("peerId"); //$NON-NLS-1$
		IPeerNode peerNode = Model.getPeerModel().getService(IPeerModelLookupService.class).lkupPeerModelById(peerId);
		if(peerNode != null) {
			String path = memento.getString("path"); //$NON-NLS-1$
			if(path == null) {
				return FSModel.getFSModel(peerNode).getRoot();
			}
			OpParsePath op = new OpParsePath(peerNode, path);
			IOpExecutor executor = new NullOpExecutor();
			IStatus status = executor.execute(op);
			if(status.isOK()) {
				return op.getResult();
			}
		}
		return null;
	}
}
