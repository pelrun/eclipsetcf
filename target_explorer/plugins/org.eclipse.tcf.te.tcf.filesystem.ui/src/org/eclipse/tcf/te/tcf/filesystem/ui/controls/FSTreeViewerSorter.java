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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.columns.FSTreeElementComparator;
import org.eclipse.tcf.te.ui.trees.TreeViewerSorterCaseInsensitive;

/**
 * File system tree control viewer sorter implementation.
 */
public class FSTreeViewerSorter extends TreeViewerSorterCaseInsensitive {
	private final FSTreeElementComparator comparator;

	/**
	 * Constructor.
	 */
	public FSTreeViewerSorter() {
		comparator = new FSTreeElementComparator();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof IFSTreeNode && e2 instanceof IFSTreeNode) {
			return comparator.compare(e1, e2);
		}
		return super.compare(viewer, e1, e2);
	}
}
