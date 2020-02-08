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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneStat;
import org.eclipse.tcf.services.IFileSystem.FileAttrs;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.remote.core.TCFFileStore;
import org.eclipse.tcf.te.tcf.remote.core.nls.Messages;
import org.eclipse.tcf.te.tcf.remote.core.operation.PeerInfo.DoneGetFileSystem;
import org.eclipse.tcf.te.tcf.remote.core.operation.PeerInfo.DoneGetUser;

public abstract class TCFFileStoreOperation<T> extends TCFOperation<T> {
	private static final Map<String, PeerInfo> fPeerInfos = new HashMap<String, PeerInfo>();

    private final TCFFileStore fFileStore;

	protected TCFFileStoreOperation(TCFFileStore fileStore) {
	    fFileStore = fileStore;
    }

	protected final TCFFileStore getFileStore() {
	    return fFileStore;
        }

	protected final String getPath() {
		return fFileStore.getPath();
	}

	protected final IPeerNode getPeerNode() {
	    return fFileStore.getConnection().getPeerNode();
    }

	private final PeerInfo getPeerInfo(IPeerNode peerNode) {
		String key = peerNode.getName();
		PeerInfo result = fPeerInfos.get(key);
		if (result == null) {
			result = new PeerInfo();
			fPeerInfos.put(key, result);
		}
		return result;
	}

	protected final void getFileSystem(DoneGetFileSystem callback) {
		IPeerNode peerNode = getPeerNode();
		if (peerNode == null) {
			setError(createStatus(format(Messages.TCFFileStoreOperation_errorNotConnected, fFileStore.getConnection().getName()), null));
		} else {
			getPeerInfo(peerNode).getFileSystem(peerNode, callback);
		}
	}

	protected final void getUser(IFileSystem fileSystem, DoneGetUser callback) {
		IPeerNode peerNode = getPeerNode();
		if (peerNode == null) {
			setError(createStatus(format(Messages.TCFFileStoreOperation_errorNotConnected, fFileStore.getConnection().getName()), null));
		} else {
			getPeerInfo(peerNode).getUser(fileSystem, callback);
		}
	}

	protected final void stat(IFileSystem fileSystem, TCFFileStore fileStore, DoneStat doneStat) {
		FileAttrs attrs = fileStore.getAttributes();
		if (attrs != null) {
			doneStat.doneStat(null, null, attrs);
		} else {
			fileSystem.stat(fileStore.getPath(), doneStat);
		}
	}
}
