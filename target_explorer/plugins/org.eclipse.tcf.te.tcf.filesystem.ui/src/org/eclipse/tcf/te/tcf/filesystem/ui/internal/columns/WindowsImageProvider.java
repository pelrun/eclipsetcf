/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.columns;

import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;

/**
 * Windows specific image provider extending the default image provider
 * to retrieve the file's images based on file extension or cached file.
 */
public class WindowsImageProvider extends DefaultImageProvider {
	// The background daemons that updates the images of the file system nodes.
	static LabelProviderUpdateDaemon updateDaemon;
	static {
		updateDaemon = new LabelProviderUpdateDaemon();
		updateDaemon.start();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.columns.DefaultImageProvider#getImage(org.eclipse.tcf.te.tcf.filesystem.model.IFSTreeNode)
	 */
	@Override
    public Image getImage(IFSTreeNode node) {
		if (node.isRootDirectory()) {
            return node.isWindowsNode() ? updateDaemon.getDiskImage() : super.getImage(node);
		}
		else if (node.isDirectory()) {
			return updateDaemon.getFolderImage();
		}
		else if(node.isFile()) {
			Image image = updateDaemon.getImage(node);
			if (image == null) {
				updateDaemon.enqueue(node);
				image = getPredefinedImage(node);
			}
			return image;
		}
	    return super.getImage(node);
    }
}
