/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.editor.tree;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.model.MessageModelNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;

/**
 * Process tree control content provider implementation.
 */
public class ContentProvider extends org.eclipse.tcf.te.tcf.processes.ui.navigator.runtime.ContentProvider {
	// The target's peer model.
	private IPeerNode peerNode;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.ui.navigator.runtime.ContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		if (peerNode != null) {
			IRuntimeModel model = ModelManager.getRuntimeModel(peerNode);
			if (model != null && model.getAutoRefreshInterval() > 0) {
				// If the model is auto refreshing, then stop it when the editor is disposed.
				model.setAutoRefreshInterval(0);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.ui.navigator.runtime.ContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		super.inputChanged(viewer, oldInput, newInput);
		peerNode = getPeerNode(newInput);
	}

    protected IPeerNode getPeerNode(Object input) {
		IPeerNode peerNode = input instanceof IPeerNode ? (IPeerNode)input : null;
		if (peerNode == null && input instanceof IAdaptable) {
			peerNode = (IPeerNode)((IAdaptable)input).getAdapter(IPeerNode.class);
		}
		return peerNode;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.processes.ui.navigator.runtime.ContentProvider#getElements(java.lang.Object)
     */
    @Override
    public Object[] getElements(Object inputElement) {
		if (peerNode != null && peerNode.getConnectState() == IConnectable.STATE_CONNECTED) {
	        return super.getElements(inputElement);
		}

		String message = null;
		if (peerNode != null) {
			if (peerNode.getConnectState() == IConnectable.STATE_CONNECTION_LOST ||
						peerNode.getConnectState() == IConnectable.STATE_CONNECTION_RECOVERING) {
				message = Messages.getStringDelegated(peerNode, "ProcessMonitor_ContentProvider_connectionLost"); //$NON-NLS-1$
			}
			if (message == null) {
				message = Messages.getStringDelegated(peerNode, "ProcessMonitor_ContentProvider_notConnected"); //$NON-NLS-1$
			}
		}

		return new Object[] { new MessageModelNode(message != null ? message : Messages.ContentProvider_notConnected, IStatus.INFO, false) };
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.ui.navigator.runtime.ContentProvider#isRuntimeModelNodeVisible()
	 */
	@Override
	protected boolean isRuntimeModelNodeVisible() {
	    return false;
	}
}
