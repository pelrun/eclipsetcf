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


/**
 * Case insensitive common sorter implementation.
 */
public class TreeViewerSorterCaseInsensitive extends TreeViewerSorter {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.trees.TreeViewerSorter#isCaseSensitve()
	 */
	@Override
    protected boolean isCaseSensitve() {
		return false;
	}

}
