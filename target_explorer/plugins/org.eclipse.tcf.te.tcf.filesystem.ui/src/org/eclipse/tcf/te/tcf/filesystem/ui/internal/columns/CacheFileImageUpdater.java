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

import java.io.File;

import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.model.ModelManager;

/**
 * The image update adapter that updates the images of the file which
 * has a local cache copy.
 */
public class CacheFileImageUpdater implements ImageUpdateAdapter {
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.columns.ImageUpdateAdapter#getImageKey(org.eclipse.tcf.te.tcf.filesystem.model.IFSTreeNode)
	 */
	@Override
    public String getImageKey(IFSTreeNode node) {
	    return node.getLocationURL().toExternalForm();
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.columns.ImageUpdateAdapter#getMirrorFile(org.eclipse.tcf.te.tcf.filesystem.model.IFSTreeNode)
	 */
	@Override
	public File getMirrorFile(IFSTreeNode node) {
	    return node.getCacheFile();
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.columns.ImageUpdateAdapter#getImgFile(org.eclipse.tcf.te.tcf.filesystem.model.IFSTreeNode)
	 */
	@Override
	public File getImageFile(IFSTreeNode node) {
		File cacheFile = node.getCacheFile();
		File parentDir = cacheFile.getParentFile();
		if (!parentDir.exists() && !parentDir.mkdirs()) {
			parentDir = ModelManager.getCacheRoot();
		}
		return new File(parentDir, node.getName() + ".png"); //$NON-NLS-1$
	}
}
