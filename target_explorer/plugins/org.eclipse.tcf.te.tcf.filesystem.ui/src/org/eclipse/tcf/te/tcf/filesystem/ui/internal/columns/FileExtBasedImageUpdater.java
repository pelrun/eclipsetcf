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
import java.io.IOException;

import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;

/**
 * The image update adapter that updates the images of the file which does
 * not have a local cache copy. The algorithm is based its extension.
 */
public class FileExtBasedImageUpdater implements ImageUpdateAdapter {
	// The label provider update daemon
	private LabelProviderUpdateDaemon updateDaemon;

	/**
	 * Create an instance with the specified daemon.
	 *
	 * @param daemon The label provider update daemon.
	 */
	public FileExtBasedImageUpdater(LabelProviderUpdateDaemon daemon) {
		this.updateDaemon = daemon;
	}

	/**
	 * Get the node's file extension or null if there is no extension.
	 *
	 * @param node The file tree node.
	 * @return The file's extension or null.
	 */
	private String getFileExt(IFSTreeNode node) {
		String name = node.getName();
		String ext = "noext"; //$NON-NLS-1$
		int index = name.lastIndexOf("."); //$NON-NLS-1$
		if (index != -1) ext = name.substring(index + 1);
		return ext;
	}

	/**
	 * Get the directory to store the temporary mirror files.
	 *
	 * @return The directory to contain the mirror files.
	 */
	private File getMirrorDir() {
		File tmpDir = updateDaemon.getTempDir();
		File mrrDir = new File(tmpDir, ".mrr"); //$NON-NLS-1$
		if(!mrrDir.exists() && !mrrDir.mkdirs()) {
			mrrDir = tmpDir;
		}
		return mrrDir;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.columns.ImageUpdateAdapter#getImageKey(org.eclipse.tcf.te.tcf.filesystem.model.IFSTreeNode)
	 */
	@Override
	public String getImageKey(IFSTreeNode node) {
		String ext = getFileExt(node);
		return "EXT_IMAGE@" + ext; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.columns.ImageUpdateAdapter#getMirrorFile(org.eclipse.tcf.te.tcf.filesystem.model.IFSTreeNode)
	 */
	@Override
	public File getMirrorFile(IFSTreeNode node) {
		String ext = getFileExt(node);
		File mrrDir = getMirrorDir();
		File file = new File(mrrDir, "mirror" + "." + ext); //$NON-NLS-1$ //$NON-NLS-2$
		if (!file.exists()) {
			try {
				file.createNewFile();
			}
			catch (IOException e) {
			}
		}
		return file;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.columns.ImageUpdateAdapter#getImgFile(org.eclipse.tcf.te.tcf.filesystem.model.IFSTreeNode)
	 */
	@Override
	public File getImageFile(IFSTreeNode node) {
		String ext = getFileExt(node);
	    return updateDaemon.getTempImg(ext);
    }
}
