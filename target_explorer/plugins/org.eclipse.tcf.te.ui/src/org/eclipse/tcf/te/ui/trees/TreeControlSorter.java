/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.trees;

import java.util.Comparator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * The tree control sorter determines if the elements to sort are from the same
 * content contribution. If not, elements from the main content provider comes
 * first and the elements from different content contributions are sorted by rank.
 */
public class TreeControlSorter extends TreeViewerSorterCaseInsensitive {
	// Reference to the parent tree control
	private final AbstractTreeControl parentTreeControl;


	/**
	 * Constructor
	 *
	 * @param parentTreeControl The parent tree control. Must not be <code>null</code>.
	 */
	public TreeControlSorter(AbstractTreeControl parentTreeControl) {
		super();
		Assert.isNotNull(parentTreeControl);
		this.parentTreeControl = parentTreeControl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.trees.TreeViewerSorter#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		ContentDescriptor c1 = null;
		ContentDescriptor c2 = null;

		int inverter = doDetermineInverter(viewer);

		ContentDescriptor[] descriptors = parentTreeControl.getContentDescriptors();
		if (descriptors != null) {
			for (ContentDescriptor descriptor : descriptors) {
				if (descriptor.getContentContribution() == null) continue;
				if (descriptor.getContentContribution().isElementHandled(e1)) {
					c1 = descriptor;
					break;
				}
			}

			for (ContentDescriptor descriptor : descriptors) {
				if (descriptor.getContentContribution() == null) continue;
				if (descriptor.getContentContribution().isElementHandled(e2)) {
					c2 = descriptor;
					break;
				}
			}
		}

		if (c1 == null && c2 == null && viewer instanceof TreeViewer) {
			// Both elements are from the main content provider, check if
			// the current column has an comparator associated.
			Tree tree = ((TreeViewer) viewer).getTree();
			TreeColumn treeColumn = tree.getSortColumn();
			if (treeColumn == null) {
				// If the sort column is not set, then use the first column.
				treeColumn = tree.getColumn(0);
			}
			if (treeColumn != null && !treeColumn.isDisposed()) {
				ColumnDescriptor column = (ColumnDescriptor) treeColumn.getData();
				if (column != null) {
					Comparator<Object> comparator = column.getComparator();
					if (comparator != null) {
						return inverter * comparator.compare(e1, e2);
					}
				}
			}
		}

		if (c1 == null && c2 != null || c1 != null && c2 == null) {
			// At least one of the elements is from the main content provider
			return inverter * (c1 == null ? -1 : 1);
		}

		if (c1 != null && c2 != null && !c1.equals(c2)) {
			// Different contributions, the rank decides the order
			return inverter * Integer.valueOf(c1.getRank()).compareTo(Integer.valueOf(c2.getRank()));
		}

	    return super.compare(viewer, e1, e2);
	}
}
