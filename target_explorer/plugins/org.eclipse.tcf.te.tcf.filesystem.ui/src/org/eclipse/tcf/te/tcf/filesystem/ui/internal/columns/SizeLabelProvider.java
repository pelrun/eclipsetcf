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

import java.text.DecimalFormat;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;

/**
 * The label provider for the tree column "size".
 */
public class SizeLabelProvider extends LabelProvider {
	// The size formatter.
	private static final DecimalFormat SIZE_FORMAT = new DecimalFormat();

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		if (element instanceof IFSTreeNode) {
			IFSTreeNode node = (IFSTreeNode) element;
			// Directory nodes does not have a size
			if (node.isFile()) {
				return SIZE_FORMAT.format(node.getSize() / 1024) + " KB"; //$NON-NLS-1$
			}
		}
		return ""; //$NON-NLS-1$
	}
}
