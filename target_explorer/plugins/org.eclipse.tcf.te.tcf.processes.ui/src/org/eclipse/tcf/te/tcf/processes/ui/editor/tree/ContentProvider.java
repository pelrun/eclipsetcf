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
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.processes.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.ui.navigator.runtime.ContentProviderDelegate;

/**
 * Process tree control content provider implementation.
 */
public class ContentProvider extends ContentProviderDelegate {
	// The target's peer model.
	private IPeerModel peerModel;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.ui.navigator.runtime.ContentProviderDelegate#dispose()
	 */
	@Override
	public void dispose() {
		if (peerModel != null) {
			IRuntimeModel model = ModelManager.getRuntimeModel(peerModel);
			if (model != null && model.getAutoRefreshInterval() > 0) {
				// If the model is auto refreshing, then stop it when the editor is disposed.
				model.setAutoRefreshInterval(0);
			}
			ModelManager.disposeRuntimeModel(peerModel);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.ui.navigator.runtime.ContentProviderDelegate#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		super.inputChanged(viewer, oldInput, newInput);
		if (newInput instanceof IPeerModel) {
			peerModel = (IPeerModel) newInput;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.ui.navigator.runtime.ContentProviderDelegate#isRuntimeModelNodeVisible()
	 */
	@Override
	protected boolean isRuntimeModelNodeVisible() {
	    return false;
	}
}
