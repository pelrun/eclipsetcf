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

import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.utils.Host;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.ui.trees.PendingAwareLabelProvider;

/**
 * The label provider for the tree column "name".
 */
public class FSTreeElementLabelProvider extends PendingAwareLabelProvider {
	// The image provider to provide platform specific images.
	private ImageProvider imgProvider;

	/**
	 * Constructor.
	 */
	public FSTreeElementLabelProvider() {
		if(Host.isWindowsHost()) {
			imgProvider = new WindowsImageProvider();
		}
		else {
			imgProvider = new DefaultImageProvider();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		if (element instanceof IFSTreeNode) {
			return ((IFSTreeNode) element).getName();
		}
		else if (element instanceof IModelNode) {
			return ((IModelNode)element).getName();
		}

		return super.getText(element);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		if (element instanceof IFSTreeNode) {
			IFSTreeNode node = (IFSTreeNode) element;
			return imgProvider.getImage(node);
		}
		else if (element instanceof IModelNode) {
			return UIPlugin.getImage(((IModelNode)element).getImageId());
		}

		return super.getImage(element);
	}
}
