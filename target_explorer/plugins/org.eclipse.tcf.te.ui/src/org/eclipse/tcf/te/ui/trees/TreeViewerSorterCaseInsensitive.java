/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
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
