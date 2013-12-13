/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.editor.tree;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;

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
		if (newInput instanceof IPeerNode) {
			peerNode = (IPeerNode) newInput;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.ui.navigator.runtime.ContentProvider#isRuntimeModelNodeVisible()
	 */
	@Override
	protected boolean isRuntimeModelNodeVisible() {
	    return false;
	}
}
