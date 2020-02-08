/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.columns;

import java.io.Serializable;
import java.util.Comparator;

import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;

/**
 * The base comparator for all the file system tree column.
 */
public abstract class FSTreeNodeComparator implements Comparator<Object>, Serializable {
    private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public final int compare(Object o1, Object o2) {
		if (!(o1 instanceof IFSTreeNode) || !(o2 instanceof IFSTreeNode))
			return 0;

		IFSTreeNode node1 = (IFSTreeNode)o1;
		IFSTreeNode node2 = (IFSTreeNode)o2;

		// Group directories and files always together before sorting by name
		boolean d1 = node1.isDirectory();
		boolean d2 = node2.isDirectory();
		if (d1 != d2)
			return d1 ? -1 : 1;

		return doCompare(node1, node2);
	}

	/**
	 * Sort the node1 and node2 when they are both directories or files.
	 *
	 * @param node1 The first node.
	 * @param node2 The second node.
	 * @return The comparison result.
	 */
	public abstract int doCompare(IFSTreeNode node1, IFSTreeNode node2);
}
