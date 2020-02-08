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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DirEntry;
import org.eclipse.tcf.services.IFileSystem.DoneRoots;
import org.eclipse.tcf.services.IFileSystem.DoneStat;
import org.eclipse.tcf.services.IFileSystem.FileAttrs;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.tcf.core.concurrent.TCFOperationMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel.Delegate;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.FileState;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.PersistenceManager;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * FSRefresh refreshes a specified tree node and its children and grand children recursively.
 */
public class OpRefresh extends AbstractOperation {
	private static Map<FSTreeNode, TCFOperationMonitor<?>> fPendingResults = new HashMap<FSTreeNode, TCFOperationMonitor<?>>();

	final LinkedList<FSTreeNode> fWork = new LinkedList<FSTreeNode>();
	final boolean fRecursive;
	private long fStartTime;

	public OpRefresh(FSTreeNode node, boolean recursive) {
		fWork.add(node);
		fRecursive = recursive;
	}

	public OpRefresh(List<IFSTreeNode> nodes, boolean recursive) {
		fRecursive = recursive;
		for (IFSTreeNode node : nodes) {
			if (node instanceof FSTreeNode) {
				fWork.add((FSTreeNode) node);
			}
		}
	}

	@Override
	public IStatus doRun(IProgressMonitor monitor) {
		FSTreeNode needsNotification = null;
		FSTreeNode trigger = fWork.peek();
		fStartTime = System.currentTimeMillis();
		monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
		while (!fWork.isEmpty()) {
			FSTreeNode node = fWork.remove();
			boolean isTop = trigger == node;
			if (isTop) {
				if (needsNotification != null) {
					needsNotification.notifyChange();
				}
				needsNotification = node;
				trigger = fWork.peek();
			}
			IStatus s = refreshNode(node, isTop, monitor);
			if (!s.isOK()) {
				if (needsNotification != null)
					needsNotification.notifyChange();
				return s;
			}
		}
		if (needsNotification != null)
			needsNotification.notifyChange();
		return Status.OK_STATUS;
	}

	private IStatus refreshNode(final FSTreeNode node, final boolean isTop, IProgressMonitor monitor) {
		if (node.getLastRefresh() >= fStartTime) {
			FSTreeNode[] children = node.getChildren();
			if (children != null) {
				for (FSTreeNode child : children) {
					fWork.addFirst(child);
				}
			}
			return Status.OK_STATUS;
		}

		boolean isDir = node.isDirectory();
		boolean isFile = node.isFile();

		if (!node.isFileSystem() && !isDir && !isFile)
			return Status.OK_STATUS;

		if (!isTop && !isFile && node.getChildren() == null)
			return Status.OK_STATUS;

		monitor.subTask(format(Messages.OpRefresh_name, node.getLocation()));
		IStatus status;
		synchronized (fPendingResults) {
			TCFOperationMonitor<?> result = fPendingResults.get(node);
			if (result == null) {
				result = new TCFOperationMonitor<Object>(false);
				fPendingResults.put(node, result);
				scheduleTcfRefresh(node, isTop, result);
			}
			status = result.waitDone(monitor);
			if (!result.hasWaiters()) {
				result.setCancelled();
				fPendingResults.remove(node);
			}
		}
		return status;
	}

	private void scheduleTcfRefresh(final FSTreeNode node, final boolean isTop, final TCFOperationMonitor<?> result) {
		if (!result.checkCancelled()) {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					if (node.isFileSystem()) {
						tcfRefreshRoots(node, result);
					} else if (isTop && !node.isRootDirectory()) {
						tcfStatAndRefresh(node, result);
					} else {
						tcfRefresh(node, result);
					}
				}
			});
		}
	}

	protected void tcfRefreshRoots(final FSTreeNode node, final TCFOperationMonitor<?> result) {
		if (!result.checkCancelled()) {
			final IFileSystem fs = node.getRuntimeModel().getFileSystem();
			if (fs == null) {
				result.setCancelled();
				return;
			}

			fs.roots(new DoneRoots() {
				@Override
				public void doneRoots(IToken token, FileSystemException error, DirEntry[] entries) {
					if (node.getRuntimeModel().getChannel() == null || node.getRuntimeModel().getChannel().getState() != IChannel.STATE_CLOSED) {
        					if (error != null) {
        						result.setError(format(Messages.OpRefresh_errorGetRoots, node.getRuntimeModel().getName()), error);
        					} else if (!result.checkCancelled()) {
        						Delegate delegate = node.getRuntimeModel().getDelegate();
        						List<FSTreeNode> nodes = new ArrayList<FSTreeNode>(entries.length);
        						for (DirEntry entry : entries) {
        							if (delegate.filterRoot(entry)) {
        								nodes.add(new FSTreeNode(node, entry.filename, true, entry.attrs));
        							}
        						}
        						node.setContent(nodes.toArray(new FSTreeNode[nodes.size()]), false);
        						for (FSTreeNode node : node.getChildren()) {
        							if (fRecursive || node.isFile()) {
        								fWork.addFirst(node);
        							}
        						}
        						result.setDone(null);
        					}
					}
				}
			});
		}
	}

	protected void tcfStatAndRefresh(final FSTreeNode node, final TCFOperationMonitor<?> result) {
		if (!result.checkCancelled()) {
			final IFileSystem fs = node.getRuntimeModel().getFileSystem();
			if (fs == null) {
				result.setCancelled();
				return;
			}

			fs.stat(node.getLocation(true), new DoneStat() {
				@Override
				public void doneStat(IToken token, FileSystemException error, FileAttrs attrs) {
					if (error != null) {
						handleFSError(node, Messages.OpRefresh_errorReadAttributes, error, result);
					} else {
						node.setAttributes(attrs, false);
						tcfRefresh(node, result);
					}
				}
			});
		}
	}

	protected void tcfRefresh(final FSTreeNode node, final TCFOperationMonitor<?> result) {
		if (!result.checkCancelled()) {
			if (node.isFile()) {
				tcfUpdateCacheDigest(node, result);
			} else if (node.isDirectory()) {
				final String path = node.getLocation(true);
				final IFileSystem fs = node.getRuntimeModel().getFileSystem();
				if (fs == null) {
					result.setCancelled();
					return;
				}

				tcfReadDir(fs, path, new IReadDirDone() {
					@Override
					public void error(FileSystemException error) {
						node.setContent(NO_CHILDREN, false);
						result.setError(format(Messages.OpRefresh_errorOpenDir, path), error);
					}

					@Override
					public boolean checkCancelled() {
						return result.checkCancelled();
					}

					@Override
					public void done(List<DirEntry> entries) {
						int i = 0;
						FSTreeNode[] nodes = new FSTreeNode[entries.size()];
						for (DirEntry entry : entries) {
							nodes[i++] = new FSTreeNode(node, entry.filename, false, entry.attrs);
						}
						node.setContent(nodes, false);
						for (FSTreeNode node : node.getChildren()) {
							if (fRecursive || node.isFile()) {
								fWork.addFirst(node);
							}
						}
						result.setDone(null);
					}
				});
			} else {
				result.setDone(null);
			}
		}
	}

	protected void tcfUpdateCacheDigest(final FSTreeNode node, final TCFOperationMonitor<?> result) {
		File cacheFile = node.getCacheFile();
		if (!cacheFile.exists()) {
			result.setDone(null);
			return;
		}

		final FileState digest = PersistenceManager.getInstance().getFileDigest(node);
		final long cacheMTime = cacheFile.lastModified();
		if (digest.getCacheDigest() == null || digest.getCacheMTime() != cacheMTime) {
			final OpCacheFileDigest op = new OpCacheFileDigest(node);
			op.runInJob(new Callback() {
				@Override
				protected void internalDone(Object caller, IStatus status) {
					if (status.isOK()) {
						digest.updateCacheDigest(op.getDigest(), cacheMTime);
						tcfUpdateTargetDigest(digest, node, result);
					} else {
						result.setDone(status, null);
					}
				}
			});
		} else {
			tcfUpdateTargetDigest(digest, node, result);
		}
	}


	protected void tcfUpdateTargetDigest(FileState digest, final FSTreeNode node, final TCFOperationMonitor<?> result) {
		if (digest.getTargetDigest() == null || digest.getTargetMTime() != node.getModificationTime()) {
			final IOperation op = node.operationDownload(new OutputStream() {
				@Override
				public void write(int b) {
				}
			});
			op.runInJob(new Callback() {
				@Override
                protected void internalDone(Object caller, IStatus status) {
					result.setDone(status, null);
                }
			});
		} else {
			result.setDone(null);
		}
	}

	@Override
    public String getName() {
	    return NLS.bind(Messages.OpRefresh_name, ""); //$NON-NLS-1$
    }
}
