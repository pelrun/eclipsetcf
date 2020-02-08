/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.navigator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.tcf.te.ui.trees.TreeViewerSorterCaseInsensitive;
import org.eclipse.tcf.te.ui.views.interfaces.ICategory;
import org.eclipse.tcf.te.ui.views.navigator.nodes.NewWizardNode;

/**
 * Category viewer sorter implementation.
 */
public class ViewerSorter extends TreeViewerSorterCaseInsensitive {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.trees.TreeViewerSorter#doCompare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object, java.lang.String, int, int)
	 */
	@Override
	protected int doCompare(Viewer viewer, Object node1, Object node2, String sortColumn, int index, int inverter) {
		if (node1 instanceof ICategory && node2 instanceof ICategory) {
			int rank1 = ((ICategory)node1).getRank();
			int rank2 = ((ICategory)node2).getRank();

			if (rank1 != -1 && rank2 != -1 && rank1 != rank2) {
				return (rank1 - rank2) * inverter;
			}
		}

		if (node1 instanceof NewWizardNode && !(node2 instanceof NewWizardNode)) {
			return -1;
		}
		if (node2 instanceof NewWizardNode && !(node1 instanceof NewWizardNode)) {
			return 1;
		}

	    return super.doCompare(viewer, node1, node2, sortColumn, index, inverter);
	}
}
