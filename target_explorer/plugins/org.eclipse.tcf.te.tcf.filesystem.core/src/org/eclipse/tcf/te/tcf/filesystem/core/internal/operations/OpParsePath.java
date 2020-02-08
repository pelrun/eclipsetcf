/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.operations;

import static java.text.MessageFormat.format;
import static org.eclipse.tcf.te.tcf.filesystem.core.model.ModelManager.getRuntimeModel;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.utils.Host;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IResultOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.testers.TargetPropertyTester;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.CacheManager;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;

/**
 * The operation to parse a platform specific path to a target's node.
 */
public class OpParsePath extends AbstractOperation implements IResultOperation<IFSTreeNode> {
	// The peer on which the file is located.
	IPeerNode peer;
	// The path on the target.
	String path;
	// The parsing result.
	FSTreeNode result;

	/**
	 * Create an instance with a path on a specified target.
	 *
	 * @param peer The target peer.
	 * @param path The path to be parsed.
	 */
	public OpParsePath(IPeerNode peer, String path) {
		this.peer = peer;
		this.path = path;
	}

	/**
	 * The path of the cache file to be parsed.
	 *
	 * @param filePath The local cache's file.
	 */
    public OpParsePath(String filePath) {
		String cache_root = CacheManager.getCacheRoot().getAbsolutePath();
		if (filePath.startsWith(cache_root)) {
			filePath = filePath.substring(cache_root.length() + 1);
			int slash = filePath.indexOf(File.separator);
			if (slash != -1) {
				String peerId = filePath.substring(0, slash);
				peerId = peerId.replace(CacheManager.PATH_ESCAPE_CHAR, ':');

				final AtomicReference<IPeerNode> peerNode = new AtomicReference<IPeerNode>();
				final String finPeerId = peerId;

				Runnable runnable = new Runnable() {

					@Override
					public void run() {
						peerNode.set(ModelManager.getPeerModel().getService(IPeerModelLookupService.class).lkupPeerModelById(finPeerId));
					}
				};

				if (Protocol.isDispatchThread()) runnable.run();
				else Protocol.invokeAndWait(runnable);

				this.peer = peerNode.get();
				if (peer != null) {
					boolean hostWindows = Host.isWindowsHost();
					boolean windows = TargetPropertyTester.isWindows(peer);
					filePath = filePath.substring(slash + 1);
					if (hostWindows) {
						if (windows) {
							slash = filePath.indexOf(File.separator);
							if (slash != -1) {
								String disk = filePath.substring(0, slash);
								filePath = filePath.substring(slash + 1);
								disk = disk.replace(CacheManager.PATH_ESCAPE_CHAR, ':');
								filePath = disk + File.separator + filePath;
							}
						}
						else {
							filePath = "/" + filePath.replace('\\', '/'); //$NON-NLS-1$
						}
					}
					else {
						if (windows) {
							slash = filePath.indexOf(File.separator);
							if (slash != -1) {
								String disk = filePath.substring(0, slash);
								filePath = filePath.substring(slash + 1);
								disk = disk.replace(CacheManager.PATH_ESCAPE_CHAR, ':');
								filePath = disk + File.separator + filePath;
							}
							filePath = filePath.replace(File.separatorChar, '\\');
						}
						else {
							filePath = "/" + filePath; //$NON-NLS-1$
						}
					}
					path = filePath;
				}
			}
		}
	}

	@Override
	public FSTreeNode getResult() {
		return result;
	}

	@Override
	public IStatus doRun(IProgressMonitor monitor) {
		monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
		if (peer == null || path == null)
			return Status.OK_STATUS;

		IRuntimeModel rtm = getRuntimeModel(peer);
		if (rtm == null)
			return null;

		final FSTreeNode node = (FSTreeNode) rtm.getRoot();
		return findPath(node, path, monitor);
	}

	private IStatus findPath(FSTreeNode node, String path, IProgressMonitor monitor) {
		if (path == null || path.length() == 0) {
			result = node;
			return Status.OK_STATUS;
		}

		if (monitor.isCanceled())
			return Status.CANCEL_STATUS;

		path = path.replace(':', CacheManager.PATH_ESCAPE_CHAR);

		if (node.getChildren() == null) {
			IStatus status = node.operationRefresh(false).run(new SubProgressMonitor(monitor, 0));
			if (!status.isOK())
				return status;
		}

		if (node.isFileSystem()) {
			for (FSTreeNode child : node.getChildren()) {
				if (path.startsWith(child.getName().replace(':', CacheManager.PATH_ESCAPE_CHAR))) {
					return findPath(child, path.substring(child.getName().length()), monitor);
				}
			}
			return Status.OK_STATUS;
		}

		String osPathSep = node.isWindowsNode() ? "\\" : "/"; //$NON-NLS-1$ //$NON-NLS-2$
		int delim = path.indexOf(osPathSep);
		final String segment;
		if (delim == -1) {
			segment = path;
			path = null;
		} else {
			segment = path.substring(0, delim);
			path = path.substring(delim+1);
		}

		node = node.findChild(segment);
		if (node == null)
			return Status.OK_STATUS;

		return findPath(node, path, monitor);
	}

	@Override
	public String getName() {
		return format(Messages.OpParsePath_name, path);
	}
}
