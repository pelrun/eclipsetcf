/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.operations;

import static java.text.MessageFormat.format;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * The operation class that copies selected FSTreeNodes to a specify destination folder.
 */
public class OpCopyLocal extends OpCopyBase<File> {
	private static final boolean IGNORE_CASE = new File("a").equals(new File("A")); //$NON-NLS-1$ //$NON-NLS-2$
	private static final File[] NO_FILES = {};

	private final Map<File, File[]> fChildrenCache = new HashMap<File, File[]>();

	public OpCopyLocal(List<? extends IFSTreeNode> nodes, File dest, IConfirmCallback confirmCallback) {
		super(nodes, dest, confirmCallback);
	}

	@Override
	protected File findChild(File destination, String name) {
		for (File child : getChildren(destination)) {
			String childName = child.getName();
			if (IGNORE_CASE ? childName.equalsIgnoreCase(name) : childName.equals(name))
				return child;
		}
		return null;
	}

	private File[] getChildren(File destination) {
		File[] result = fChildrenCache.get(destination);
		if (result == null) {
			result = destination.listFiles();
			if (result == null)
				result = NO_FILES;
			fChildrenCache.put(destination, result);
		}
		return result;
	}

	@Override
	protected void notifyChange(File destination) {
	}

	@Override
	protected IStatus refreshDestination(File destination, long startTime, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	@Override
	protected boolean isDirectory(File node) {
		return node.isDirectory();
	}

	@Override
	protected boolean isFile(File node) {
		return node.isFile();
	}

	@Override
	protected String getLocation(File node) {
		return node.getAbsolutePath();
	}

	@Override
	protected IStatus performCopy(final FSTreeNode source, final File destination, final String newName, final File existing, IProgressMonitor monitor) {
		monitor.subTask(NLS.bind(Messages.OpCopy_Copying, source.getLocation()));
		if (monitor.isCanceled())
			return Status.CANCEL_STATUS;

		if (source.isFile()) {
			return copyFile(source, destination, newName, new SubProgressMonitor(monitor, 0));
		}
		if (source.isDirectory()) {
			return copyFolder(source, destination, newName);
		}
		return Status.OK_STATUS;
	}

	private IStatus copyFolder(final FSTreeNode source, final File dest, final String newName) {
		File newFolder = new File(dest, newName);
		if (!newFolder.mkdir())
			return StatusHelper.createStatus(format(Messages.Operation_CannotCreateDirectory, newName), null);

		fChildrenCache.remove(dest);
		addWorkItem(source.getChildren(), newFolder);
		return Status.OK_STATUS;
	}

	private IStatus copyFile(final FSTreeNode source, final File dest, final String newName, IProgressMonitor monitor) {
		File fileDest = new File(dest, newName);
		fChildrenCache.remove(dest);
		OutputStream output;
		try {
			output = new BufferedOutputStream(new FileOutputStream(fileDest));
		} catch (FileNotFoundException e) {
			return StatusHelper.createStatus(format(Messages.OpCopy_CannotCopyFile, newName), e);
		}
		IStatus result = source.operationDownload(output).run(monitor);
		try {
			output.close();
		} catch (IOException e) {
		}
		return result;
	}

	@Override
    public String getName() {
	    return Messages.OpCopy_DownloadingFile;
    }
}
