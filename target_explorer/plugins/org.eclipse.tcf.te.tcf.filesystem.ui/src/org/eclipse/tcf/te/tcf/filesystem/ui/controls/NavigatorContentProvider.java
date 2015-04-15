/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.controls;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.filesystem.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;


/**
 * The base navigator content provider for File System and Process Monitor
 */
public abstract class NavigatorContentProvider extends TreeContentProvider  implements ITreeViewerListener {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		if (element instanceof IFSTreeNode) {
			IFSTreeNode node = (IFSTreeNode) element;
			IFSTreeNode parent = node.getParent();
			if (parent != null) {
				if (parent.isFileSystem()) {
					if (isRootNodeVisible()) return parent;
					return null;
				}
				return parent;
			}
			if (isRootNodeVisible())
				return node.getPeerNode();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeViewerListener#treeCollapsed(org.eclipse.jface.viewers.TreeExpansionEvent)
	 */
	@Override
    public void treeCollapsed(TreeExpansionEvent event) {
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeViewerListener#treeExpanded(org.eclipse.jface.viewers.TreeExpansionEvent)
	 */
	@Override
    public void treeExpanded(TreeExpansionEvent event) {
//		Object object = event.getElement();
//	    if(object instanceof IFSTreeNode) {
//	    	IFSTreeNode parent = (IFSTreeNode) object;
//	    	IFSTreeNode[] children = parent.getChildren();
//	    	if (children == null) {
//	    		parent.operationRefresh(false).runInJob(null);
//	    	}
//		}
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.trees.TreeContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	    super.inputChanged(viewer, oldInput, newInput);
	    this.viewer.addTreeListener(this);
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.trees.TreeContentProvider#dispose()
	 */
	@Override
    public void dispose() {
	    this.viewer.removeTreeListener(this);
	    super.dispose();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		super.getChildren(parentElement);

		if (parentElement instanceof IPeerNode) {
			final IPeerNode peerNode = (IPeerNode)parentElement;
			IRuntimeModel model = ModelManager.getRuntimeModel(peerNode);
			if (model == null)
				return NO_ELEMENTS;

			if (isRootNodeVisible()) {
				IFSTreeNode root = model.getRoot();
				return new Object[] { root };
			}
			return getChildren(model.getRoot());
		} else if (parentElement instanceof IFSTreeNode) {
			IFSTreeNode node = (IFSTreeNode)parentElement;
			if (!(node.isDirectory() || node.isFileSystem()))
				return NO_ELEMENTS;

			IFSTreeNode[] children = node.getChildren();
			if (children == null) {
				node.operationRefresh(false).runInJob(null);
				return new Object[] {getPending(node)};
			}
			return children;
		}

		return NO_ELEMENTS;
	}

	@Override
	public boolean hasChildren(final Object element) {
		Assert.isNotNull(element);

		if (element instanceof IFSTreeNode) {
			IFSTreeNode node = (IFSTreeNode)element;
			if (node.isFileSystem() || node.isDirectory()) {
				return node.getChildren() == null || super.hasChildren(element);
			}
			return false;
		}

		if (element instanceof IPeerNode) {
			IPeerNode peerNode = (IPeerNode) element;
			IRuntimeModel model = ModelManager.getRuntimeModel(peerNode);
			return model != null;
		}
		return false;
	}

	/**
	 * If the root node of the tree is visible.
	 *
	 * @return true if it is visible.
	 */
	protected boolean isRootNodeVisible() {
		return true;
	}
}
