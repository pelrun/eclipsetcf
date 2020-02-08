/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.pages;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.filesystem.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.ui.editor.AbstractTreeViewerExplorerEditorPage;
import org.eclipse.tcf.te.ui.trees.TreeControl;
/**
 * The editor page for the file system explorer.
 */
public class FSExplorerEditorPage extends AbstractTreeViewerExplorerEditorPage {

	// The event listener instance
	private FSExplorerEventListener listener = null;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractTreeViewerExplorerEditorPage#dispose()
	 */
	@Override
	public void dispose() {
		if (listener != null) {
			EventManager.getInstance().removeEventListener(listener);
			listener = null;
		}
	    super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractTreeViewerExplorerEditorPage#getDoubleClickCommandId()
	 */
	@Override
	protected String getDoubleClickCommandId() {
		return "org.eclipse.ui.navigator.Open"; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractTreeViewerExplorerEditorPage#getViewerId()
	 */
	@Override
	protected String getViewerId() {
		return "org.eclipse.tcf.te.ui.controls.viewer.fs"; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage#getFormTitle()
	 */
	@Override
    protected String getFormTitle() {
	    return Messages.FSExplorerEditorPage_PageTitle;
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractCustomFormToolkitEditorPage#getContextHelpId()
	 */
	@Override
    protected String getContextHelpId() {
	    return "org.eclipse.tcf.te.tcf.filesystem.FSExplorerEditorPage"; //$NON-NLS-1$
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractTreeViewerExplorerEditorPage#getViewerInput()
	 */
	@Override
    protected Object getViewerInput() {
		Object element = getEditorInputNode();
		IPeerNode peerNode = element instanceof IPeerNode ? (IPeerNode)element : null;
		if (peerNode == null && element instanceof IAdaptable) {
			peerNode = (IPeerNode)((IAdaptable)element).getAdapter(IPeerNode.class);
		}
		if (peerNode != null) {
			IRuntimeModel rtModel = ModelManager.getRuntimeModel(peerNode);
			if (rtModel != null)
				return rtModel.getRoot();
			return peerNode;
		}
		return null;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.editor.pages.AbstractTreeViewerExplorerEditorPage#doCreateTreeControl()
	 */
	@Override
	protected TreeControl doCreateTreeControl() {
	    TreeControl treeControl = super.doCreateTreeControl();
	    Assert.isNotNull(treeControl);

	    if (listener == null) {
	    	listener = new FSExplorerEventListener(treeControl);
	    	EventManager.getInstance().addEventListener(listener, ChangeEvent.class);
	    }

	    return treeControl;
	}
}
