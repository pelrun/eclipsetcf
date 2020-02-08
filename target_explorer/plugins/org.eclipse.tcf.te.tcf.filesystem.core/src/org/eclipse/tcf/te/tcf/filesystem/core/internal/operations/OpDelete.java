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

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DirEntry;
import org.eclipse.tcf.services.IFileSystem.DoneRemove;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.tcf.core.concurrent.TCFOperationMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNodeWorkingCopy;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.CacheManager;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * FSDelete deletes the selected FSTreeNode list.
 */
public class OpDelete extends AbstractOperation {
	private static class WorkItem {
		final WorkItem fParent;
		final FSTreeNode fNode;
		boolean fContentCleared = false;
		boolean fContentLeftOK = false;

		WorkItem(FSTreeNode node) {
			fParent = null;
			fNode = node;
		}

		WorkItem(WorkItem item, FSTreeNode node) {
			fParent = item;
			fNode = node;
		}

		void setContentLeftOK() {
			fContentLeftOK = true;
			if (fParent != null)
				fParent.setContentLeftOK();
		}
	}

	LinkedList<WorkItem> fWork = new LinkedList<WorkItem>();
	IConfirmCallback fConfirmCallback;
	private List<FSTreeNode> fNodes;

	public OpDelete(List<? extends IFSTreeNode> nodes, IConfirmCallback confirmCallback) {
		fNodes = dropNestedNodes(nodes);
		fConfirmCallback = confirmCallback;
	}

	@Override
	protected IStatus doRun(IProgressMonitor monitor) {
		SubMonitor sm = SubMonitor.convert(monitor, getName(), fNodes.size());
		for (FSTreeNode node : fNodes) {
			IStatus status = removeNode(node, sm.newChild(1));
			node.getParent().notifyChange();
			if (!status.isOK())
				return status;
		}
		return Status.OK_STATUS;
	}


	private IStatus removeNode(FSTreeNode node, SubMonitor monitor) {
		fWork.add(new WorkItem(node));
		while (!fWork.isEmpty()) {
			IStatus s = runWorkItem(fWork.remove(), monitor);
			if (!s.isOK()) {
				node.notifyChange();
				return s;
			}
		}
		return Status.OK_STATUS;
	}

	protected IStatus runWorkItem(final WorkItem item, IProgressMonitor monitor) {
		if (item.fContentLeftOK) {
			if (item.fParent == null) {
				return item.fNode.operationRefresh(true).run(monitor);
			}
			return Status.OK_STATUS;
		}

		if (fConfirmCallback != null && fConfirmCallback.requires(item.fNode)) {
			int makeWritable = confirmCallback(item.fNode, fConfirmCallback);
			switch (makeWritable) {
			case IConfirmCallback.NO:
				item.setContentLeftOK();
				return Status.OK_STATUS;
			case IConfirmCallback.YES:
				IStatus s = mkWritable(item.fNode, monitor);
				if (!s.isOK())
					return s;
				break;
			case IConfirmCallback.CANCEL:
			default:
				return Status.CANCEL_STATUS;
			}
		}

		CacheManager.clearCache(item.fNode);

		final TCFOperationMonitor<?> result = new TCFOperationMonitor<Object>();
		monitor.subTask(NLS.bind(Messages.OpDelete_RemovingFileFolder, item.fNode.getLocation()));
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				tcfRunWorkItem(item, result);
			}
		});
		return result.waitDone(monitor);
	}

	private IStatus mkWritable(IFSTreeNode node, IProgressMonitor monitor) {
		final IFSTreeNodeWorkingCopy workingCopy = node.createWorkingCopy();
		if (node.isWindowsNode()) {
			workingCopy.setReadOnly(false);
		} else {
			workingCopy.setWritable(true);
		}
		return workingCopy.operationCommit().run(new SubProgressMonitor(monitor, 0));
	}

	protected void tcfRunWorkItem(final WorkItem item, TCFOperationMonitor<?> result) {
		if (item.fNode.isFile()) {
			tcfDeleteFile(item, result);
		} else if (item.fNode.isDirectory()) {
			if (item.fContentCleared) {
				tcfDeleteEmptyFolder(item, result);
			} else {
				tcfDeleteFolder(item, result);
			}
		} else {
			result.setDone(null);
		}
	}

	private void tcfDeleteFolder(final WorkItem item, final TCFOperationMonitor<?> result)  {
		final String path = item.fNode.getLocation(true);
		final IFileSystem fs = item.fNode.getRuntimeModel().getFileSystem();
		if (fs == null) {
			result.setCancelled();
			return;
		}

		tcfReadDir(fs, path, new IReadDirDone() {
			@Override
			public void error(FileSystemException error) {
				result.setError(format(Messages.OpDelete_error_readDir, path), error);
			}

			@Override
			public boolean checkCancelled() {
				return result.checkCancelled();
			}

			@Override
			public void done(List<DirEntry> entries) {
				// Add current work item for final deletion
				item.fContentCleared = true;
				fWork.addFirst(item);
				// Create work items for the children
				for (DirEntry entry : entries) {
					FSTreeNode node = new FSTreeNode(item.fNode, entry.filename, false, entry.attrs);
					fWork.addFirst(new WorkItem(item, node));
				}
				result.setDone(null);
			}
		});
	}

	private void tcfDeleteFile(final WorkItem item, final TCFOperationMonitor<?> result) {
		final IFileSystem fs = item.fNode.getRuntimeModel().getFileSystem();
		if (fs == null) {
			result.setCancelled();
			return;
		}
		fs.remove(item.fNode.getLocation(true), new DoneRemove() {
			@Override
			public void doneRemove(IToken token, FileSystemException error) {
				tcfHandleRemoved(item, error, result);
			}
		});
	}

	private void tcfDeleteEmptyFolder(final WorkItem item, final TCFOperationMonitor<?> result)  {
		final IFileSystem fs = item.fNode.getRuntimeModel().getFileSystem();
		if (fs == null) {
			result.setCancelled();
			return;
		}
		fs.rmdir(item.fNode.getLocation(true), new DoneRemove() {
			@Override
			public void doneRemove(IToken token, FileSystemException error) {
				tcfHandleRemoved(item, error, result);
			}
		});
	}

	protected void tcfHandleRemoved(final WorkItem item, FileSystemException error, final TCFOperationMonitor<?> result) {
		if (error != null) {
			result.setError(format(Messages.OpDelete_error_delete, item.fNode.getLocation()), error);
		} else if (!result.checkCancelled()) {
			if (item.fParent == null) {
				item.fNode.getParent().removeNode(item.fNode, true);
			}
			result.setDone(null);
		}
	}

	@Override
    public String getName() {
	    return Messages.OpDelete_Deleting;
    }
}
