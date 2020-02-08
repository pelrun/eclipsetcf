/*******************************************************************************
 * Copyright (c) 2014, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.model;

import static java.text.MessageFormat.format;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneUser;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.concurrent.TCFOperationMonitor;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.DoneOpenChannel;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IResultOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.UserAccount;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpDelete;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpParsePath;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpRefresh;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpUpload;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.CacheManager;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Filesystem service model manager implementation.
 */
public class ModelManager {
	static final Map<String, IRuntimeModel> fModels = Collections.synchronizedMap(new HashMap<String, IRuntimeModel>());

	/**
	 * Returns the runtime model instance for the given peer model
	 * <p>
	 * If not yet initialized, a new runtime model will be initialized before returning.
	 *
	 * @param peerNode The peer model instance. Must not be <code>null</code>.
	 * @return The runtime model.
	 */
	public static IRuntimeModel getRuntimeModel(final IPeerNode peerNode) {
		Assert.isNotNull(peerNode);

		IRuntimeModel model = fModels.get(peerNode.getPeerId());
		if (model != null)
			return model;

		if (peerNode.getConnectState() != IConnectable.STATE_CONNECTED)
			return null;


		final TCFOperationMonitor<IRuntimeModel> result = new TCFOperationMonitor<IRuntimeModel>();
		// Create the runnable to execute
		Runnable runnable = new Runnable() {
			@Override
            public void run() {
				Assert.isTrue(Protocol.isDispatchThread());

				IRuntimeModel model = fModels.get(peerNode.getPeerId());
				if (model != null) {
					result.setDone(model);
				} else {
					createRuntimeModel(peerNode, result);
				}
			}
		};
		// Execute the runnable
		if (Protocol.isDispatchThread()) {
			runnable.run();
		} else {
			Protocol.invokeAndWait(runnable);
			result.waitDone(null);
		}

		return result.getValue();
	}

	protected static void createRuntimeModel(final IPeerNode peerNode, final TCFOperationMonitor<IRuntimeModel> result) {
		Assert.isTrue(Protocol.isDispatchThread());
		Map<String, Boolean> flags = new HashMap<String, Boolean>();
		flags.put(IChannelManager.FLAG_FORCE_NEW, Boolean.TRUE);
		flags.put(IChannelManager.FLAG_NO_PATH_MAP, Boolean.TRUE);
		flags.put(IChannelManager.FLAG_NO_VALUE_ADD, Boolean.TRUE);

		Tcf.getChannelManager().openChannel(peerNode.getPeer(), flags, new DoneOpenChannel() {
			@Override
			public void doneOpenChannel(Throwable error, final IChannel channel) {
				if (error != null) {
					result.setError(format(Messages.ModelManager_errorOpenChannel, peerNode.getName()), error);
					return;
				}
				if (result.checkCancelled()) {
					Tcf.getChannelManager().closeChannel(channel);
					return;
				}
				final IFileSystem fs = channel.getRemoteService(IFileSystem.class);
				if (fs == null) {
					Tcf.getChannelManager().closeChannel(channel);
					result.setError(format(Messages.Operation_NoFileSystemError, peerNode.getName()), null);
					return;
				}

				fs.user(new DoneUser() {
					@Override
					public void doneUser(IToken token, FileSystemException error, int uid, int euid, int gid,
							int egid, String home) {
						if (error != null) {
							result.setError(format(Messages.ModelManager_errorNoUserAccount, peerNode.getName()), error);
							Tcf.getChannelManager().closeChannel(channel);
							return;
						}
						if (result.checkCancelled()) {
							Tcf.getChannelManager().closeChannel(channel);
							return;
						}

						String peerId = peerNode.getPeerId();
						IRuntimeModel rt = fModels.get(peerId);
						if (rt != null) {
							Tcf.getChannelManager().closeChannel(channel);
						} else {
							UserAccount account = new UserAccount(uid, gid, euid, egid, home);
							rt = new RuntimeModel(peerNode, channel, fs, account);
							fModels.put(peerId, rt);
						}
						result.setDone(rt);
					}
				});
			}
		});
	}

	/**
	 * Dispose the runtime model.
	 *
	 * @param peerNode The peer model instance. Must not be <code>null</code>.
	 */
	public static void disposeRuntimeModel(final IPeerNode peerNode) {
		Assert.isNotNull(peerNode);

		Runnable runnable = new Runnable() {
			@Override
            public void run() {
				Assert.isTrue(Protocol.isDispatchThread());

				// Get the peer id
				String id = peerNode.getPeerId();
				// Lookup the runtime model instance
				IRuntimeModel candidate = fModels.remove(id);
				if (candidate != null) {
					Tcf.getChannelManager().closeChannel(candidate.getChannel());
					candidate.dispose();
				}
			}
		};

		if (Protocol.isDispatchThread()) {
			runnable.run();
		} else {
			Protocol.invokeAndWait(runnable);
		}
	}

	public static File getCacheRoot() {
		return CacheManager.getCacheRoot();
	}

	public static IOperation operationUpload(List<IFSTreeNode> nodes) {
		OpUpload upload = new OpUpload(null);
		for (IFSTreeNode node : nodes) {
			upload.addUpload(node.getCacheFile(), (FSTreeNode) node);
		}
		return upload;
	}

	public static IResultOperation<IFSTreeNode> operationRestoreFromPath(String path) {
		return new OpParsePath(path);
	}

	public static IOperation operationDelete(List<IFSTreeNode> nodes, IConfirmCallback readonlyCallback) {
		return new OpDelete(nodes, readonlyCallback);
	}

	public static IOperation operationRefresh(List<IFSTreeNode> nodes, boolean recursive) {
		return new OpRefresh(nodes, recursive);
	}
}
