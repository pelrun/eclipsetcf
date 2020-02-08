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

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * The operation class that copies selected FSTreeNodes to a specify destination folder.
 */
public abstract class OpCopyBase<D> extends AbstractOperation {
	private static class WorkItem<D> {
		final boolean fTop;
		final D fDestination;
		final FSTreeNode[] fSources;
		WorkItem(FSTreeNode[] sources, D destination, boolean top) {
			fSources = sources;
			fDestination = destination;
			fTop = top;
		}
	}

	private IConfirmCallback fConfirmCallback;

	private LinkedList<WorkItem<D>> fWork = new LinkedList<WorkItem<D>>();
	private long fStartTime;

	/**
	 * Create a copy operation using the specified nodes and destination folder,
	 * using the specified flags of copying permissions and ownership and a callback
	 * to confirm to overwrite existing files.
	 *
	 * @param nodes The file/folder nodes to be copied.
	 * @param dest The destination folder to be copied to.
	 */
	public OpCopyBase(List<? extends IFSTreeNode> nodes, D dest, IConfirmCallback confirmCallback) {
		super();
		fConfirmCallback = confirmCallback;
		nodes = dropNestedNodes(nodes);
		fWork.add(new WorkItem<D>(nodes.toArray(new FSTreeNode[nodes.size()]), dest, true));
	}

	abstract protected void notifyChange(D destination);

	abstract protected IStatus refreshDestination(D destination, long startTime, IProgressMonitor monitor);

	abstract protected D findChild(D destination, String newName);

	abstract protected boolean isDirectory(D existing);

	abstract protected boolean isFile(D existing);

	abstract protected String getLocation(D existing);

	abstract protected IStatus performCopy(final FSTreeNode source, final D destination, final String newName, final D existing, IProgressMonitor monitor);

	protected void addWorkItem(FSTreeNode[] nodes, D dest) {
		fWork.addFirst(new WorkItem<D>(nodes, dest, false));
	}

	@Override
	public final IStatus doRun(IProgressMonitor monitor) {
		fStartTime = System.currentTimeMillis();
		monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
		WorkItem<D> lastTop = null;
		while (!fWork.isEmpty()) {
			WorkItem<D> item = fWork.remove();
			if (item.fTop) {
				if (lastTop != null)
					notifyChange(lastTop.fDestination);
				lastTop = item;
			}
			IStatus s = runWorkItem(item, monitor);
			if (!s.isOK()) {
				if (lastTop != null) {
					notifyChange(lastTop.fDestination);
				}
				return s;
			}
		}
		if (lastTop != null)
			notifyChange(lastTop.fDestination);

		return Status.OK_STATUS;
	}


	protected IStatus runWorkItem(final WorkItem<D> item, IProgressMonitor monitor) {
		final D destination = item.fDestination;
		IStatus status = refreshDestination(destination, fStartTime, monitor);
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


	private IStatus performCopy(FSTreeNode source, D destination, IProgressMonitor monitor) {
		String newName = new File(source.getName().replace(':', '$')).getName();
		D existing = findChild(destination, newName);
		if (existing != null) {
			if (source == existing) {
				newName = createNewNameForCopy(destination, newName);
				existing = null;
			} else if (source.isDirectory()) {
				if (!isDirectory(existing)) {
					return StatusHelper.createStatus(format(Messages.OpCopy_error_noDirectory, getLocation(existing)), null);
				}
				int replace = confirmCallback(existing, fConfirmCallback);
				if (replace == IConfirmCallback.NO) {
					return Status.OK_STATUS;
				}
				if (replace != IConfirmCallback.YES) {
					return Status.CANCEL_STATUS;
				}

				fWork.addFirst(new WorkItem<D>(source.getChildren(), existing, false));
				return Status.OK_STATUS;
			} else if (source.isFile()) {
				if (!isFile(existing)) {
					return StatusHelper.createStatus(format(Messages.OpCopy_error_noFile, getLocation(existing)), null);
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


	private String createNewNameForCopy(D node, String origName) {
		String name = origName;
		int n = 0;
		while (findChild(node, name) != null) {
			if (n > 0) {
				name = NLS.bind(Messages.Operation_CopyNOfFile, Integer.valueOf(n), origName);
			} else {
				name = NLS.bind(Messages.Operation_CopyOfFile, origName);
			}
			n++;
		}
		return name;
	}

	@Override
    public String getName() {
	    return Messages.OpCopy_CopyingFile;
    }
}
