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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneRemove;
import org.eclipse.tcf.services.IFileSystem.DoneRename;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.tcf.core.concurrent.TCFOperationMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.CacheManager;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * FSMove moves specified tree nodes to a destination folder.
 */
public class OpMove extends AbstractOperation {
	private static class WorkItem {
		final WorkItem fParent;
		final FSTreeNode fDestination;
		final FSTreeNode fSource;
		boolean fContentCleared = false;
		boolean fContentLeftOK = false;

		WorkItem(FSTreeNode source, FSTreeNode destination) {
			this(null, source, destination);
		}

		WorkItem(WorkItem parent, FSTreeNode source, FSTreeNode destination) {
			fParent = parent;
			fSource = source;
			fDestination = destination;
		}

		void setContentLeftOK() {
			fContentLeftOK = true;
			if (fParent != null)
				fParent.setContentLeftOK();
		}

	}

	IConfirmCallback fConfirmCallback;

	LinkedList<WorkItem> fWork = new LinkedList<WorkItem>();
	private long fStartTime;

	public OpMove(List<? extends IFSTreeNode> nodes, FSTreeNode dest, IConfirmCallback confirmCallback) {
		fConfirmCallback = confirmCallback;
		for (FSTreeNode node : dropNestedNodes(nodes)) {
			fWork.add(new WorkItem(node, dest));
		}
	}

	@Override
	public String getName() {
	    return Messages.OpMove_MovingFile;
	}

	@Override
	public IStatus doRun(IProgressMonitor monitor) {
		if (fWork.isEmpty())
			return Status.OK_STATUS;

		fStartTime = System.currentTimeMillis();
		monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);

		List<FSTreeNode> notify = new ArrayList<FSTreeNode>();
		notify.add(fWork.peek().fDestination);
		for (WorkItem item : fWork) {
			notify.add(item.fSource.getParent());
		}

		IStatus status = Status.OK_STATUS;
		while (!fWork.isEmpty()) {
			WorkItem item = fWork.remove();
			status = runWorkItem(item, monitor);
			if (!status.isOK())
				break;
		}

		for (FSTreeNode node : dropNestedNodes(notify)) {
			node.notifyChange();
		}
		return status;
	}

	protected IStatus runWorkItem(final WorkItem item, final IProgressMonitor monitor) {
		if (item.fContentLeftOK)
			return Status.OK_STATUS;

		if (item.fContentCleared) {
			return deleteEmptyFolder(item.fSource, monitor);
		}

		return move(item, monitor);
	}

	private IStatus move(final WorkItem item, final IProgressMonitor monitor) {
		monitor.subTask(NLS.bind(Messages.OpMove_Moving, item.fSource.getLocation()));

		final FSTreeNode source = item.fSource;
		final FSTreeNode destination = item.fDestination;
		IStatus status = refresh(destination, fStartTime, monitor);
		if (!status.isOK())
			return status;

		status = refresh(source, fStartTime, monitor);
		if (!status.isOK())
			return status;

		final FSTreeNode existing = destination.findChild(source.getName());
		if (existing != null) {
			if (source == existing) {
				return Status.OK_STATUS;
			}
			if (source.isDirectory()) {
				if (!existing.isDirectory()) {
					return StatusHelper.createStatus(format(Messages.OpCopy_error_noDirectory, existing.getLocation()), null);
				}
				int replace = confirmCallback(existing, fConfirmCallback);
				if (replace == IConfirmCallback.NO) {
					item.setContentLeftOK();
					return Status.OK_STATUS;
				}
				if (replace != IConfirmCallback.YES) {
					return Status.CANCEL_STATUS;
				}

				item.fContentCleared = true;
				fWork.addFirst(item);
				for (FSTreeNode child : source.getChildren()) {
					fWork.addFirst(new WorkItem(item, child, existing));
				}
				return Status.OK_STATUS;
			} else if (source.isFile()) {
				if (!existing.isFile()) {
					return StatusHelper.createStatus(format(Messages.OpCopy_error_noFile, existing.getLocation()), null);
				}
				int replace = confirmCallback(existing, fConfirmCallback);
				if (replace == IConfirmCallback.NO) {
					item.setContentLeftOK();
					return Status.OK_STATUS;
				}
				if (replace != IConfirmCallback.YES) {
					return Status.CANCEL_STATUS;
				}
			} else {
				return Status.OK_STATUS;
			}
		}

		CacheManager.clearCache(existing);
		CacheManager.clearCache(source);

		final TCFOperationMonitor<?> result = new TCFOperationMonitor<Object>();
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (existing != null) {
					tcfMoveReplace(source, destination, existing, result);
				} else {
					tcfMove(source, destination, result);
				}
			}
		});
		return result.waitDone(monitor);
	}


	protected void tcfMoveReplace(final FSTreeNode source, final FSTreeNode destination, final FSTreeNode existing, final TCFOperationMonitor<?> result) {
		if (result.checkCancelled())
			return;

		final IFileSystem fileSystem = destination.getRuntimeModel().getFileSystem();
		if (fileSystem == null) {
			result.setCancelled();
			return;
		}

		fileSystem.remove(existing.getLocation(true), new DoneRemove() {
			@Override
			public void doneRemove(IToken token, FileSystemException error) {
				if (error != null) {
					result.setError(format(Messages.OpMove_CannotMove, source.getLocation()), error);
				} else if (!result.checkCancelled()) {
					existing.getParent().removeNode(existing, false);
					tcfMove(source, destination, result);
				}
			}
		});
	}

	protected void tcfMove(final FSTreeNode source, final FSTreeNode dest, final TCFOperationMonitor<?> result) {
		final IFileSystem fileSystem = dest.getRuntimeModel().getFileSystem();
		if (fileSystem == null) {
			result.setCancelled();
			return;
		}

		final String sourcePath = source.getLocation(true);
		final String destPath = getPath(dest, source.getName());

		fileSystem.rename(sourcePath, destPath, new DoneRename() {
			@Override
			public void doneRename(IToken token, FileSystemException error) {
				if (error != null) {
					result.setError(format(Messages.OpMove_CannotMove, sourcePath), error);
				} else {
					source.getParent().removeNode(source, false);
					source.changeParent(dest);
					dest.addNode(source, false);
					result.setDone(null);
				}
			}
		});
	}

	private IStatus deleteEmptyFolder(final FSTreeNode source, IProgressMonitor monitor) {
		CacheManager.clearCache(source);
		final TCFOperationMonitor<?> result = new TCFOperationMonitor<Object>();
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				tcfDeleteEmptyFolder(source, result);
			}
		});
		return result.waitDone(monitor);
	}

	protected void tcfDeleteEmptyFolder(final FSTreeNode source, final TCFOperationMonitor<?> result)  {
		final IFileSystem fs = source.getRuntimeModel().getFileSystem();
		if (fs == null) {
			result.setCancelled();
			return;
		}
		fs.rmdir(source.getLocation(true), new DoneRemove() {
			@Override
			public void doneRemove(IToken token, FileSystemException error) {
				if (error != null) {
					result.setError(format(Messages.OpDelete_error_delete, source.getLocation()), error);
				} else if (!result.checkCancelled()) {
					source.getParent().removeNode(source, false);
					result.setDone(null);
				}
			}
		});
	}
}
