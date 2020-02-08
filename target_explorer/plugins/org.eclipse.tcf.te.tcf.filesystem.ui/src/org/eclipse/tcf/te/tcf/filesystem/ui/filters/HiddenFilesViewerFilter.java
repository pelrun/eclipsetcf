/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.filters;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;

/**
 * A filter implementation filtering hidden files or directories.
 */
public class HiddenFilesViewerFilter extends ViewerFilter {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		// The element needs to be a tree node, but not a root node
		if (element instanceof IFSTreeNode && !((IFSTreeNode)element).isRootDirectory()) {
			IFSTreeNode node = (IFSTreeNode) element;
			if(node.isWindowsNode()) {
				return !node.isHidden();
			}
			return !node.getName().startsWith("."); //$NON-NLS-1$
		}
		// Let pass all other elements unharmed
		return true;
	}

}
