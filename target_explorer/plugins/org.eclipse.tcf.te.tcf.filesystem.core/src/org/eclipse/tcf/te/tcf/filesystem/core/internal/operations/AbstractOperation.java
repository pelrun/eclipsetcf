/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.operations;

import static java.util.Arrays.asList;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DirEntry;
import org.eclipse.tcf.services.IFileSystem.DoneClose;
import org.eclipse.tcf.services.IFileSystem.DoneOpen;
import org.eclipse.tcf.services.IFileSystem.DoneReadDir;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.services.IFileSystem.IFileHandle;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.tcf.core.concurrent.TCFOperationMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

public abstract class AbstractOperation implements IOperation {
	protected static final FSTreeNode[] NO_CHILDREN = {};

	public interface IReadDirDone {

		void error(FileSystemException error);

		boolean checkCancelled();

		void done(List<DirEntry> entries);

	}


	private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,##0.##"); //$NON-NLS-1$
	protected static final int DEFAULT_CHUNK_SIZE = 5 * 1024;

	private int fStandardAnswer = -1;

	protected abstract IStatus doRun(IProgressMonitor monitor);

	@Override
	public final IStatus run(IProgressMonitor monitor) {
		if (monitor == null)
			monitor = new NullProgressMonitor();
		try {
			return doRun(monitor);
		} finally {
			monitor.done();
		}
	}

	@Override
	public final void runInJob(ICallback callback) {
		runInJob(false, callback);
	}

	@Override
	public final void runInUserJob(ICallback callback) {
		runInJob(true, callback);
	}

	private final void runInJob(boolean user, final ICallback callback) {
		Job job = new Job(getName()){
			@Override
            protected IStatus run(IProgressMonitor monitor) {
				return AbstractOperation.this.run(monitor);
            }
		};
		if (callback != null) {
			job.addJobChangeListener(new JobChangeAdapter(){
				@Override
				public void done(final IJobChangeEvent event) {
					callback.done(AbstractOperation.this, event.getResult());
				}
			});
		}
		job.setUser(user);
		job.schedule();
	}

	protected String getPath(FSTreeNode node, String childName) {
		String path = node.getLocation(true);
		if (path.charAt(path.length()-1) == '/')
			return path + childName;
		return path + '/' + childName;
	}

	protected List<FSTreeNode> dropNestedNodes(List<? extends IFSTreeNode> nodes) {
		List<FSTreeNode> result = new ArrayList<FSTreeNode>();
		for (IFSTreeNode n : nodes) {
			addWithoutNested(result, n);
		}
		return result;
	}

	private void addWithoutNested(List<FSTreeNode> result, IFSTreeNode newNode) {
		if (!(newNode instanceof FSTreeNode))
			return;
		for (ListIterator<FSTreeNode> it = result.listIterator(); it.hasNext(); ) {
			FSTreeNode node = it.next();
			if (node == newNode || node.isAncestorOf(newNode))
				return;
			if (newNode.isAncestorOf(node)) {
				it.set((FSTreeNode) newNode);
				return;
			}
		}
		result.add((FSTreeNode) newNode);
	}

	protected IStatus refresh(FSTreeNode node, long olderThan, IProgressMonitor monitor) {
		if (node.getLastRefresh() < olderThan)
			return node.operationRefresh(false).run(new SubProgressMonitor(monitor, 0));
		return Status.OK_STATUS;
	}


	protected int confirmCallback(final Object node, IConfirmCallback confirmCallback) {
		if (confirmCallback == null)
			return IConfirmCallback.YES;

		if (fStandardAnswer >= 0)
			return fStandardAnswer;

		int answer = confirmCallback.confirms(node);
		switch (answer) {
		case IConfirmCallback.CANCEL:
		case IConfirmCallback.NO:
		case IConfirmCallback.YES:
			return answer;
		case IConfirmCallback.NO_TO_ALL:
			fStandardAnswer = IConfirmCallback.NO;
			return fStandardAnswer;
		case IConfirmCallback.YES_TO_ALL:
			fStandardAnswer = IConfirmCallback.YES;
			return fStandardAnswer;
		default:
			return IConfirmCallback.CANCEL;
		}
	}

	/**
	 * Use the SIZE_FORMAT to format the file's size. The rule is: 1. If the
	 * size is less than 1024 bytes, then show it as "####" bytes. 2. If the
	 * size is less than 1024 KBs, while more than 1 KB, then show it as
	 * "####.##" KBs. 3. If the size is more than 1 MB, then show it as
	 * "####.##" MBs.
	 *
	 * @param size
	 *            The file size to be displayed.
	 * @return The string representation of the size.
	 */
	protected String formatSize(long size) {
		double kbSize = size / 1024.0;
		if (kbSize < 1.0) {
			return SIZE_FORMAT.format(size) + Messages.OpStreamOp_Bytes;
		}
		double mbSize = kbSize / 1024.0;
		if (mbSize < 1.0)
			return SIZE_FORMAT.format(kbSize) + Messages.OpStreamOp_KBs;
		return SIZE_FORMAT.format(mbSize) + Messages.OpStreamOp_MBs;
	}


	protected void handleFSError(final FSTreeNode node, String msg, FileSystemException error, final TCFOperationMonitor<?> result) {
		int status = error.getStatus();
		if (status == IFileSystem.STATUS_NO_SUCH_FILE) {
			node.getParent().removeNode(node, true);
			result.setDone(null);
		} else {
			node.setContent(NO_CHILDREN, false);
			result.setError(msg, error);
		}
	}

	protected void tcfReadDir(final IFileSystem fs, String path, final IReadDirDone callback) {
		fs.opendir(path, new DoneOpen() {
			private IFileHandle fHandle;
			protected List<DirEntry> fEntries;

			@Override
			public void doneOpen(IToken token, FileSystemException error, final IFileHandle handle) {
				if (error != null) {
					callback.error(error);
				} else {
					fHandle = handle;
					if (callback.checkCancelled()) {
						cleanup();
					} else {
						fEntries = new ArrayList<DirEntry>();
						readDir();
					}
				}
			}

			protected void readDir() {
				fs.readdir(fHandle, new DoneReadDir() {
					@Override
					public void doneReadDir(IToken token, FileSystemException error, DirEntry[] entries, boolean eof) {
						if (error != null) {
							cleanup();
							callback.error(error);
						} else if (callback.checkCancelled()) {
							cleanup();
						} else {
							fEntries.addAll(asList(entries));
							if (eof) {
								cleanup();
								callback.done(fEntries);
							} else {
								readDir();
							}
						}
					}
				});
			}

			protected void cleanup() {
				if (fHandle != null) {
					fs.close(fHandle, new DoneClose() {
						@Override
						public void doneClose(IToken token, FileSystemException error) {
						}
					});
				}
			}
		});
	}
}
