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

import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DirEntry;
import org.eclipse.tcf.services.IFileSystem.DoneRoots;
import org.eclipse.tcf.services.IFileSystem.DoneStat;
import org.eclipse.tcf.services.IFileSystem.FileAttrs;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.tcf.core.concurrent.Rendezvous;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.FileState;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.PersistenceManager;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

/**
 * FSRefresh refreshes a specified tree node and its children and grand children recursively.
 */
public class OpRefresh extends AbstractOperation {
	static final FSTreeNode[] NO_CHILDREN = {};
	private static Map<FSTreeNode, TCFResult<?>> fPendingResults = new HashMap<FSTreeNode, TCFResult<?>>();

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

		final Rendezvous rendezvous;
		if (!isTop) {
			rendezvous = null;
			if (isFile || node.getChildren() == null)
				return Status.OK_STATUS;
		} else {
			if (isFile) {
			    FileState digest = PersistenceManager.getInstance().getFileDigest(node);
				rendezvous = new Rendezvous();
				digest.updateState(new Callback(){
					@Override
		            protected void internalDone(Object caller, IStatus status) {
						rendezvous.arrive();
		            }
				});
			} else {
				rendezvous = null;
			}
		}

		monitor.subTask(format(Messages.OpRefresh_name, node.getLocation()));
		IStatus status;
		synchronized (fPendingResults) {
			TCFResult<?> result = fPendingResults.get(node);
			if (result == null) {
				result = new TCFResult<Object>(false);
				fPendingResults.put(node, result);
				scheduleTcfRefresh(node, isTop, result);
			}
			status = result.waitDone(monitor);
			if (!result.hasWaiters()) {
				result.setCancelled();
				fPendingResults.remove(node);
			}
		}

		if (rendezvous != null) {
			try {
				rendezvous.waiting(10000);
			} catch (TimeoutException e) {
			}
		}
		return status;
	}

	private void scheduleTcfRefresh(final FSTreeNode node, final boolean isTop, final TCFResult<?> result) {
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (node.isFileSystem()) {
					tcfRefreshRoots(node, result);
				} else if (isTop && !node.isRootDirectory()) {
					tcfStatAndRefresh(node, result);
				} else {
					tcfRefreshDir(node, result);
				}
			}
		});
	}

	protected void tcfStatAndRefresh(final FSTreeNode node, final TCFResult<?> result) {
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
						if (!attrs.isDirectory()) {
							node.operationDownload(new OutputStream() {
								@Override
								public void write(int b) {}
							}).runInJob(new Callback() {
								@Override
								protected void internalDone(Object caller, IStatus status) {
									result.setDone(null);
								}
							});
							result.setDone(null);
						} else if (!result.checkCancelled()){
							tcfRefreshDir(node, result);
						}
					}
				}
			});
		}
	}

	protected void tcfRefreshRoots(final FSTreeNode node, final TCFResult<?> result) {
		if (!result.checkCancelled()) {
			final IFileSystem fs = node.getRuntimeModel().getFileSystem();
			if (fs == null) {
				result.setCancelled();
				return;
			}

			fs.roots(new DoneRoots() {
				@Override
				public void doneRoots(IToken token, FileSystemException error, DirEntry[] entries) {
					if (error != null) {
						result.setError(format(Messages.OpRefresh_errorGetRoots, node.getRuntimeModel().getName()), error);
					} else if (!result.checkCancelled()) {
						int i = 0;
						FSTreeNode[] nodes = new FSTreeNode[entries.length];
						for (DirEntry entry : entries) {
							nodes[i++] = new FSTreeNode(node, entry.filename, true, entry.attrs);
						}
						node.setContent(nodes, false);
						if (fRecursive) {
							for (FSTreeNode node : nodes) {
								fWork.addFirst(node);
							}
						}
						result.setDone(null);
					}
				}
			});
		}
	}

	protected void tcfRefreshDir(final FSTreeNode node, final TCFResult<?> result) {
		if (!result.checkCancelled()) {
			final String path = node.getLocation(true);
			final IFileSystem fs = node.getRuntimeModel().getFileSystem();
			if (fs == null) {
				result.setCancelled();
				return;
			}

			tcfReadDir(fs, path, new IReadDirDone() {
				@Override
				public void error(FileSystemException error) {
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
					if (fRecursive) {
						for (FSTreeNode node : nodes) {
							fWork.addFirst(node);
						}
					}
					result.setDone(null);
				}
			});
		}
	}

	protected void handleFSError(final FSTreeNode node, String msg, FileSystemException error, final TCFResult<?> result) {
		int status = error.getStatus();
		if (status == IFileSystem.STATUS_NO_SUCH_FILE) {
			node.getParent().removeNode(node, true);
			result.setDone(null);
		} else {
			node.setContent(NO_CHILDREN, false);
			result.setDone(null);
		}
	}

	@Override
    public String getName() {
	    return NLS.bind(Messages.OpRefresh_name, ""); //$NON-NLS-1$
    }
}
