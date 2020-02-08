/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.celleditor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.widgets.Item;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations.UiExecutor;

/**
 * FSCellModifier is an <code>ICellModifier</code> of the file system tree of the target explorer.
 */
public class FSCellModifier implements ICellModifier {
	// The column property used to get the name of a given file system node.
	public static final String PROPERTY_NAME = "name"; //$NON-NLS-1$

	public FSCellModifier() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean canModify(Object element, String property) {
		if (property.equals(PROPERTY_NAME)) {
			if (element instanceof Item) {
				element = ((Item) element).getData();
			}
			if (element instanceof IFSTreeNode) {
				IFSTreeNode node = (IFSTreeNode) element;
				if (!node.isRootDirectory()) {
					return node.isWindowsNode() && !node.isReadOnly() || !node.isWindowsNode() && node.isWritable();
				}
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
	 */
	@Override
	public Object getValue(Object element, String property) {
		if (property.equals(PROPERTY_NAME)) {
			if (element instanceof Item) {
				element = ((Item) element).getData();
			}
			if (element instanceof IFSTreeNode) {
				IFSTreeNode node = (IFSTreeNode) element;
				return node.getName();
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void modify(Object element, String property, Object value) {
		if (property.equals(PROPERTY_NAME)) {
			if (element instanceof Item) {
				element = ((Item) element).getData();
			}
			if (element instanceof IFSTreeNode) {
				IFSTreeNode node = (IFSTreeNode) element;
				Assert.isTrue(value != null && value instanceof String);
				String newName = (String) value;
				UiExecutor.execute(node.operationRename(newName));
			}
		}
	}
}
