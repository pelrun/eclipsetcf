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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;


/**
 * File system content provider for the common navigator of Target Explorer.
 */
public class FSNavigatorContentProvider extends NavigatorContentProvider {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.trees.TreeContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
    public void inputChanged(final Viewer viewer, Object oldInput, Object newInput) {
	    super.inputChanged(viewer, oldInput, newInput);
	    UIPlugin.getClipboard().addPropertyChangeListener(commonViewerListener);
	    UIPlugin plugin = UIPlugin.getDefault();
		IPreferenceStore preferenceStore = plugin.getPreferenceStore();
		preferenceStore.addPropertyChangeListener(commonViewerListener);
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.trees.TreeContentProvider#dispose()
	 */
	@Override
    public void dispose() {
	    UIPlugin.getClipboard().removePropertyChangeListener(commonViewerListener);
	    UIPlugin plugin = UIPlugin.getDefault();
		IPreferenceStore preferenceStore = plugin.getPreferenceStore();
		preferenceStore.removePropertyChangeListener(commonViewerListener);
	    super.dispose();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IFSTreeNode) {
			IFSTreeNode node = (IFSTreeNode)parentElement;
			if (node.isFile()) return NO_ELEMENTS;
		}
		return super.getChildren(parentElement);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(final Object element) {
		if (element instanceof IFSTreeNode) {
			IFSTreeNode node = (IFSTreeNode)element;
			if(node.isFile()) {
				return false;
			}
		}
		return super.hasChildren(element);
	}
}
