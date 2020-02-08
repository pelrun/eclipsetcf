/*******************************************************************************
 * Copyright (c) 2014, 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.remote.core.operation;

import static java.text.MessageFormat.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IChannel.IChannelListener;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneUser;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager.DoneOpenChannel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.remote.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.remote.core.nls.Messages;

public final class PeerInfo implements IChannelListener {
	private IFileSystem fFileSystem;
	private boolean fPendingOpen;
	private List<DoneGetFileSystem> fCallbacks = emptyList();
	private User fUser;

	public interface DoneGetFileSystem {
		void done(IFileSystem fileSystem, IStatus status);
    }

	public void getFileSystem(final IPeerNode peerNode, final DoneGetFileSystem callback)  {
		assert Protocol.isDispatchThread();

		if (peerNode.getConnectState() != IConnectable.STATE_CONNECTED) {
			callback.done(null, errorStatus(format(Messages.PeerInfo_errorPeerNotConnected, peerNode.getName()), null));
		} else if (fFileSystem != null) {
			callback.done(fFileSystem, Status.OK_STATUS);
		} else {
			openChannel(peerNode, callback);
		}
	}

	public void openChannel(final IPeerNode peerNode, final DoneGetFileSystem callback) {
	    if (fCallbacks.isEmpty()) {
	    	fCallbacks = singletonList(callback);
	    } else {
	    	if (fCallbacks.size() == 1) {
	    		fCallbacks = new ArrayList<DoneGetFileSystem>(fCallbacks);
	    	}
	    	fCallbacks.add(callback);
	    }
	    if (fPendingOpen)
	    	return;

	    // Open the channel
		fPendingOpen = true;
		final IPeer peer = peerNode.getPeer();
		Map<String, Boolean> flags = new HashMap<String, Boolean>();
		flags.put(IChannelManager.FLAG_NO_PATH_MAP, Boolean.TRUE);
		flags.put(IChannelManager.FLAG_FORCE_NEW, Boolean.TRUE);
		Tcf.getChannelManager().openChannel(peer, flags, new DoneOpenChannel() {
			@Override
			@SuppressWarnings("synthetic-access")
			public void doneOpenChannel(Throwable error, IChannel channel) {
				fPendingOpen = false;
				IStatus fsStatus = Status.OK_STATUS;
				if (error != null) {
					fsStatus = errorStatus(format(Messages.PeerInfo_errorCannotOpenChannel, peer.getName()), error);
				} else {
					channel.addChannelListener(PeerInfo.this);
					fFileSystem = channel.getRemoteService(IFileSystem.class);
					if (fFileSystem == null) {
						fsStatus = errorStatus(format(Messages.PeerInfo_errorNoFileSystemService, peer.getName()), null);
					}
				}
				for (DoneGetFileSystem callback : fCallbacks) {
					callback.done(fFileSystem, fsStatus);
				}
			}
		});
	}

	private IStatus errorStatus(String msg, Throwable cause) {
	    return new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), msg, cause);
    }

	@Override
	public void congestionLevel(int level) {
	}

	@Override
	public void onChannelOpened() {
	}

	@Override
	public void onChannelClosed(Throwable error) {
		// Connection was closed, next time we attempt to open a channel again.
		fFileSystem = null;
		fCallbacks = emptyList();
	}

	public interface DoneGetUser {
		void done(User user, IStatus status);
    }

	public final class User {
		public final int fEffectiveGID;
		public final int fEffectiveUID;

		User(int effectiveUID, int effectiveGID) {
			fEffectiveUID = effectiveUID;
			fEffectiveGID = effectiveGID;
		}
	}

	public void getUser(final IFileSystem fs, final DoneGetUser callback)  {
		assert Protocol.isDispatchThread();
		if (fUser != null) {
			callback.done(fUser, Status.OK_STATUS);
			return;
		}
		fs.user(new DoneUser() {
			@Override
			@SuppressWarnings("synthetic-access")
			public void doneUser(IToken token, FileSystemException error, int realUID, int effectiveUID, int realGID, int effectiveGID, String home) {
				if (error != null) {
					callback.done(null, errorStatus(error.getMessage(), error));
				} else {
					fUser = new User(effectiveUID, effectiveGID);
					callback.done(fUser, Status.OK_STATUS);
				}
			}
		});
	}
}
