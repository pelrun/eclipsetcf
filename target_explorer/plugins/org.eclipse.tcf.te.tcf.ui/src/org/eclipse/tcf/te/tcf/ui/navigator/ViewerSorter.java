/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.navigator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Category viewer sorter implementation.
 */
public class ViewerSorter extends org.eclipse.tcf.te.ui.views.navigator.ViewerSorter {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.trees.TreeViewerSorter#doCompare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object, java.lang.String, int, int)
	 */
	@Override
	protected int doCompare(Viewer viewer, Object node1, Object node2, String sortColumn, int index, int inverter) {

		if (node1 instanceof IPeerNode && node2 instanceof IPeerNode) {
			IPeerNode o1 = (IPeerNode)node1;
			String type1 = o1.getPeerType();
			type1 = type1 != null ? type1 : ""; //$NON-NLS-1$
			IPeerNode o2 = (IPeerNode)node2;
			String type2 = o2.getPeerType();
			type2 = type2 != null ? type2 : ""; //$NON-NLS-1$
			int typeCompare = type1.compareTo(type2);
			if (typeCompare != 0) {
			    return typeCompare * inverter;
			}
		}

	    return super.doCompare(viewer, node1, node2, sortColumn, index, inverter);
	}
}
