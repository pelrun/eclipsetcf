/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.operations;

import static java.text.MessageFormat.format;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneCopy;
import org.eclipse.tcf.services.IFileSystem.DoneMkDir;
import org.eclipse.tcf.services.IFileSystem.DoneStat;
import org.eclipse.tcf.services.IFileSystem.FileAttrs;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.tcf.core.concurrent.TCFOperationMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * The operation class that copies selected FSTreeNodes to a specify destination folder.
 */
public class OpCopy extends OpCopyBase<FSTreeNode> {
	private final boolean fCopyPermissions;
	private final boolean fCopyOwnership;

	public OpCopy(List<? extends IFSTreeNode> nodes, FSTreeNode dest, boolean cpPerm, boolean cpOwn, IConfirmCallback confirmCallback) {
		super(nodes, dest, confirmCallback);
		fCopyOwnership = cpOwn;
		fCopyPermissions = cpPerm;
	}

	@Override
	protected FSTreeNode findChild(FSTreeNode destination, String name) {
		return destination.findChild(name);
	}

	@Override
	protected void notifyChange(FSTreeNode destination) {
		destination.notifyChange();
	}

	@Override
	protected IStatus refreshDestination(FSTreeNode destination, long startTime, IProgressMonitor monitor) {
		return refresh(destination, startTime, monitor);
	}

	@Override
	protected boolean isDirectory(FSTreeNode node) {
		return node.isDirectory();
	}

	@Override
	protected boolean isFile(FSTreeNode node) {
		return node.isFile();
	}

	@Override
	protected String getLocation(FSTreeNode node) {
		return node.getLocation();
	}

	@Override
	protected IStatus performCopy(final FSTreeNode source, final FSTreeNode destination, final String newName, final FSTreeNode existing, IProgressMonitor monitor) {
		final TCFOperationMonitor<?> result = new TCFOperationMonitor<Object>();
		monitor.subTask(NLS.bind(Messages.OpCopy_Copying, source.getLocation()));
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				tcfPerformCopy(source, destination, newName, existing, result);
			}
		});
		return result.waitDone(monitor);
	}


	protected void tcfPerformCopy(FSTreeNode source, FSTreeNode destination, String newName, FSTreeNode existing, TCFOperationMonitor<?> result) {
		if (result.checkCancelled())
			return;

		if (source.isFile()) {
			tcfCopyFile(source, destination, newName, existing, result);
		} else if (source.isDirectory()) {
			tcfCopyFolder(source, destination, newName, result);
		} else {
			result.setDone(null);
		}
	}

	private void tcfCopyFolder(final FSTreeNode source, final FSTreeNode dest, final String newName, final TCFOperationMonitor<?> result) {
		final IFileSystem fileSystem = dest.getRuntimeModel().getFileSystem();
		if (fileSystem == null) {
			result.setCancelled();
			return;
		}

		final String path = getPath(dest, newName);
		fileSystem.mkdir(path, source.getAttributes(), new DoneMkDir() {
			@Override
			public void doneMkDir(IToken token, FileSystemException error) {
				if (error != null) {
					result.setError(StatusHelper.createStatus(format(Messages.Operation_CannotCreateDirectory, newName), error));
				} else if (!result.checkCancelled()) {
					fileSystem.lstat(path, new DoneStat() {
						@Override
						public void doneStat(IToken token, FileSystemException error, FileAttrs attrs) {
							if (error != null) {
								result.setError(StatusHelper.createStatus(format(Messages.Operation_CannotCreateDirectory, newName), error));
							} else if (!result.checkCancelled()) {
								FSTreeNode copy = new FSTreeNode(dest, newName, false, attrs);
								copy.setContent(new FSTreeNode[0], false);
								dest.addNode(copy, false);
								addWorkItem(source.getChildren(), copy);
								result.setDone(null);
							}
						}
					});
				}
			}
		});
	}

	private void tcfCopyFile(final FSTreeNode source, final FSTreeNode dest, final String newName, final FSTreeNode existing, final TCFOperationMonitor<?> result) {
		final IFileSystem fileSystem = dest.getRuntimeModel().getFileSystem();
		if (fileSystem == null) {
			result.setCancelled();
			return;
		}

		String sourcePath = source.getLocation(true);
		final String path = getPath(dest, newName);
		fileSystem.copy(sourcePath, path, fCopyPermissions, fCopyOwnership, new DoneCopy() {
			@Override
			public void doneCopy(IToken token, FileSystemException error) {
				if (error != null) {
					result.setError(StatusHelper.createStatus(format(Messages.OpCopy_CannotCopyFile, source.getName()), error));
				} else if (!result.checkCancelled()) {
					fileSystem.stat(path, new DoneStat() {
						@Override
						public void doneStat(IToken token, FileSystemException error, FileAttrs attrs) {
							if (error != null) {
								result.setError(StatusHelper.createStatus(format(Messages.OpCopy_CannotCopyFile, source.getName()), error));
							} else if (!result.checkCancelled()) {
								if (existing != null) {
									existing.setAttributes(attrs, false);
								} else {
									dest.addNode(new FSTreeNode(dest, newName, false, attrs), false);
								}
								result.setDone(null);
							}
						}
					});
				}
			}
		});
	}

	@Override
    public String getName() {
	    return Messages.OpCopy_CopyingFile;
    }
}
