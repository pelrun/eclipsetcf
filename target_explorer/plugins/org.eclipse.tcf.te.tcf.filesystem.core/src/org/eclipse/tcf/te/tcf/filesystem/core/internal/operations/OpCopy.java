/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.operations;

import static java.text.MessageFormat.format;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneCopy;
import org.eclipse.tcf.services.IFileSystem.DoneMkDir;
import org.eclipse.tcf.services.IFileSystem.DoneStat;
import org.eclipse.tcf.services.IFileSystem.FileAttrs;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * The operation class that copies selected FSTreeNodes to a specify destination folder.
 */
public class OpCopy extends AbstractOperation {
	private static class WorkItem {
		final boolean fTop;
		final FSTreeNode fDestination;
		final FSTreeNode[] fSources;
		WorkItem(FSTreeNode[] sources, FSTreeNode destination, boolean top) {
			fSources = sources;
			fDestination = destination;
			fTop = top;
		}
	}

	IConfirmCallback fConfirmCallback;
	boolean fCopyPermissions;
	boolean fCopyOwnership;

	LinkedList<WorkItem> fWork = new LinkedList<WorkItem>();
	private long fStartTime;

	/**
	 * Create a copy operation using the specified nodes and destination folder,
	 * using the specified flags of copying permissions and ownership and a callback
	 * to confirm to overwrite existing files.
	 *
	 * @param nodes The file/folder nodes to be copied.
	 * @param dest The destination folder to be copied to.
	 */
	public OpCopy(List<? extends IFSTreeNode> nodes, FSTreeNode dest, boolean cpPerm, boolean cpOwn, IConfirmCallback confirmCallback) {
		super();
		fCopyOwnership = cpOwn;
		fCopyPermissions = cpPerm;
		fConfirmCallback = confirmCallback;
		nodes = dropNestedNodes(nodes);
		fWork.add(new WorkItem(nodes.toArray(new FSTreeNode[nodes.size()]), dest, true));
	}

	@Override
	public IStatus doRun(IProgressMonitor monitor) {
		fStartTime = System.currentTimeMillis();
		monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
		WorkItem lastTop = null;
		while (!fWork.isEmpty()) {
			WorkItem item = fWork.remove();
			if (item.fTop) {
				if (lastTop != null)
					lastTop.fDestination.notifyChange();
				lastTop = item;
			}
			IStatus s = runWorkItem(item, monitor);
			if (!s.isOK()) {
				lastTop.fDestination.notifyChange();
				return s;
			}
		}
		if (lastTop != null)
			lastTop.fDestination.notifyChange();
		return Status.OK_STATUS;
	}

	protected IStatus runWorkItem(final WorkItem item, IProgressMonitor monitor) {
		final FSTreeNode destination = item.fDestination;
		IStatus status = refresh(destination, fStartTime, monitor);
		if (!status.isOK()) {
			return status;
		}

		for (FSTreeNode source : item.fSources) {
			status = refresh(source, fStartTime, monitor);
			if (!status.isOK()) {
				return status;
			}

			status = performCopy(source, destination, monitor);
			if (!status.isOK())
				return status;
		}
		return Status.OK_STATUS;
	}

	private IStatus performCopy(FSTreeNode source, FSTreeNode destination, IProgressMonitor monitor) {
		String newName = source.getName();
		FSTreeNode existing = destination.findChild(newName);
		if (existing != null) {
			if (source == existing) {
				newName = createNewNameForCopy(destination, newName);
				existing = null;
			} else if (source.isDirectory()) {
				if (!existing.isDirectory()) {
					return StatusHelper.createStatus(format(Messages.OpCopy_error_noDirectory, existing.getLocation()), null);
				}
				int replace = confirmCallback(existing, fConfirmCallback);
				if (replace == IConfirmCallback.NO) {
					return Status.OK_STATUS;
				}
				if (replace != IConfirmCallback.YES) {
					return Status.CANCEL_STATUS;
				}

				fWork.addFirst(new WorkItem(source.getChildren(), existing, false));
				return Status.OK_STATUS;
			} else if (source.isFile()) {
				if (!existing.isFile()) {
					return StatusHelper.createStatus(format(Messages.OpCopy_error_noFile, existing.getLocation()), null);
				}
				int replace = confirmCallback(existing, fConfirmCallback);
				if (replace == IConfirmCallback.NO) {
					return Status.OK_STATUS;
				}
				if (replace != IConfirmCallback.YES) {
					return Status.CANCEL_STATUS;
				}
			} else {
				return Status.OK_STATUS;
			}
		}
		return performCopy(source, destination, newName, existing, monitor);
	}


	private String createNewNameForCopy(FSTreeNode node, String origName) {
		String name = origName;
		int n = 0;
		while (node.findChild(name) != null) {
			if (n > 0) {
				name = NLS.bind(Messages.Operation_CopyNOfFile, Integer.valueOf(n), origName);
			} else {
				name = NLS.bind(Messages.Operation_CopyOfFile, origName);
			}
			n++;
		}
		return name;
	}

	private IStatus performCopy(final FSTreeNode source, final FSTreeNode destination, final String newName, final FSTreeNode existing, IProgressMonitor monitor) {
		final TCFResult<?> result = new TCFResult<Object>();
		monitor.subTask(NLS.bind(Messages.OpCopy_Copying, source.getLocation()));
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				tcfPerformCopy(source, destination, newName, existing, result);
			}
		});
		return result.waitDone(monitor);
	}


	protected void tcfPerformCopy(FSTreeNode source, FSTreeNode destination, String newName, FSTreeNode existing, TCFResult<?> result) {
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

	private void tcfCopyFolder(final FSTreeNode source, final FSTreeNode dest, final String newName, final TCFResult<?> result) {
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
					result.setError(format(Messages.Operation_CannotCreateDirectory, newName), error);
				} else if (!result.checkCancelled()) {
					fileSystem.lstat(path, new DoneStat() {
						@Override
						public void doneStat(IToken token, FileSystemException error, FileAttrs attrs) {
							if (error != null) {
								result.setError(format(Messages.Operation_CannotCreateDirectory, newName), error);
							} else if (!result.checkCancelled()) {
								FSTreeNode copy = new FSTreeNode(dest, newName, false, attrs);
								copy.setContent(new FSTreeNode[0], false);
								dest.addNode(copy, false);
								fWork.addFirst(new WorkItem(source.getChildren(), copy, false));
								result.setDone(null);
							}
						}
					});
				}
			}
		});
	}

	private void tcfCopyFile(final FSTreeNode source, final FSTreeNode dest, final String newName, final FSTreeNode existing, final TCFResult<?> result) {
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
					result.setError(format(Messages.OpCopy_CannotCopyFile, source.getName()), error);
				} else if (!result.checkCancelled()) {
					fileSystem.stat(path, new DoneStat() {
						@Override
						public void doneStat(IToken token, FileSystemException error, FileAttrs attrs) {
							if (error != null) {
								result.setError(format(Messages.OpCopy_CannotCopyFile, source.getName()), error);
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
