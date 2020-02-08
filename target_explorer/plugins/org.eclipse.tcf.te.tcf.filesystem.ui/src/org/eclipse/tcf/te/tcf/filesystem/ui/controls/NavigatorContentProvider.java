/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.controls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IResultOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.filesystem.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;


/**
 * The base navigator content provider for File System and Process Monitor
 */
public abstract class NavigatorContentProvider extends TreeContentProvider {

	private Set<IRuntimeModel> fModelsWithOpenFavorites = new HashSet<IRuntimeModel>();

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

	private void checkOpenFavorites(IRuntimeModel rtm) {
		if (!fModelsWithOpenFavorites.add(rtm))
			return;

		final IResultOperation<IFSTreeNode[]> operation = rtm.operationRestoreFavorites();
		operation.runInJob(new Callback() {
			@Override
			protected void internalDone(Object caller, IStatus status) {
				IFSTreeNode[] nodes = operation.getResult();
				if (nodes != null) {
					final List<IFSTreeNode> expandMe = new ArrayList<IFSTreeNode>();
					for (IFSTreeNode node : nodes) {
						while ((node = node.getParent()) != null) {
							expandMe.add(node);
						}
					}
					Collections.reverse(expandMe);
					viewer.getControl().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							Set<IFSTreeNode> handled = new HashSet<IFSTreeNode>();
							for (IFSTreeNode n : expandMe) {
								if (handled.add(n))
									viewer.setExpandedState(n, true);
							}
						}
					});
				}
			}
		});
    }

	@Override
	public Object[] getChildren(Object parentElement) {
		super.getChildren(parentElement);

		if (parentElement instanceof IPeerNode) {
			final IPeerNode peerNode = (IPeerNode)parentElement;
			IRuntimeModel model = ModelManager.getRuntimeModel(peerNode);
			if (model == null)
				return NO_ELEMENTS;

			checkOpenFavorites(model);
			if (isRootNodeVisible()) {
				IFSTreeNode root = model.getRoot();
				return new Object[] { root };
			}
			return getChildren(model.getRoot());
		} else if (parentElement instanceof IFSTreeNode) {
			IFSTreeNode node = (IFSTreeNode)parentElement;
			checkOpenFavorites(node.getRuntimeModel());

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
