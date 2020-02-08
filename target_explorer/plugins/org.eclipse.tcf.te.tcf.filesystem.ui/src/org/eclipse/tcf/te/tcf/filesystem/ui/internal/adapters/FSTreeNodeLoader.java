/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.adapters;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.ui.interfaces.ILazyLoader;

/**
 * The implementation of ILazyLoader for IFSTreeNode check its data availability
 * and load its children if not ready.
 */
public class FSTreeNodeLoader implements ILazyLoader {
	// The node to be checked.
	private IFSTreeNode node;
	/**
	 * Constructor
	 *
	 * @param node The file/folder node.
	 */
	public FSTreeNodeLoader(IFSTreeNode node) {
		this.node = node;
    }

	@Override
	public boolean isDataLoaded() {
		return node.getChildren() != null;
	}

	@Override
	public void loadData(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		node.operationRefresh(false).run(monitor);
	}

	@Override
    public boolean isLeaf() {
		IFSTreeNode[] children = node.getChildren();
		if (children != null) {
			return children.length == 0;
		}
		return node.isFile();
    }
}
