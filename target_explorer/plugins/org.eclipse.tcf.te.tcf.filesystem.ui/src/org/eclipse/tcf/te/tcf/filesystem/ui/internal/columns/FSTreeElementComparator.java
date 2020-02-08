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

import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;

/**
 * The comparator for the tree column "name".
 */
public class FSTreeElementComparator extends FSTreeNodeComparator {
    private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.columns.FSTreeNodeComparator#doCompare(org.eclipse.tcf.te.tcf.filesystem.model.IFSTreeNode, org.eclipse.tcf.te.tcf.filesystem.model.IFSTreeNode)
	 */
	@Override
	public int doCompare(IFSTreeNode node1, IFSTreeNode node2) {
		String name1 = node1.getName();
		String name2 = node2.getName();
		if (name1 != null && name2 != null) {
			if (name1.startsWith(".") && !name2.startsWith(".")) return -1; //$NON-NLS-1$ //$NON-NLS-2$
			if (!name1.startsWith(".") && name2.startsWith(".")) return 1; //$NON-NLS-1$ //$NON-NLS-2$
			return name1.compareToIgnoreCase(name2);
		}
		return 0;
	}
}
