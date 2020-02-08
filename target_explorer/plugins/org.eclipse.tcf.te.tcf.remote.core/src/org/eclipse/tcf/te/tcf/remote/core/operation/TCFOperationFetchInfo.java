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

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneStat;
import org.eclipse.tcf.services.IFileSystem.FileAttrs;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.tcf.remote.core.TCFFileStore;
import org.eclipse.tcf.te.tcf.remote.core.operation.PeerInfo.DoneGetFileSystem;
import org.eclipse.tcf.te.tcf.remote.core.operation.PeerInfo.DoneGetUser;
import org.eclipse.tcf.te.tcf.remote.core.operation.PeerInfo.User;

public final class TCFOperationFetchInfo extends TCFFileStoreOperation<IFileInfo> {

    public TCFOperationFetchInfo(TCFFileStore filestore) {
	    super(filestore);
    }

    @Override
    protected void doExecute() {
    	getFileSystem(new DoneGetFileSystem() {
    		@Override
    		public void done(final IFileSystem fileSystem, IStatus status) {
    			if (shallAbort(status))
    				return;
    			getUser(fileSystem, new DoneGetUser() {
    				@Override
    				public void done(final User user, IStatus status) {
    					if (shallAbort(status))
    						return;

    					stat(fileSystem, getFileStore(), new DoneStat() {
    						@Override
    						public void doneStat(IToken token, FileSystemException error, FileAttrs attrs) {
    							if (error != null) {
    								if (error.getStatus() == IFileSystem.STATUS_NO_SUCH_FILE) {
    									FileInfo result = new FileInfo(getFileStore().getName());
    									result.setExists(false);
    									setResult(result);
    								} else {
    									setError(error);
    								}
    							} else {
    								getFileStore().setAttributes(attrs);
    								setResult(user, attrs);
    							}
    						}
    					});
    				}
    			});
    		}
    	});
	}

	protected final void setResult(final User user, FileAttrs attrs) {
        FileInfo result = new FileInfo(getFileStore().getName());
        result.setExists(true);
        int flags= attrs.flags;
        if ((flags & IFileSystem.ATTR_SIZE) != 0) {
        	result.setLength(attrs.size);
        }
        if ((flags & IFileSystem.ATTR_ACMODTIME) != 0) {
        	result.setLastModified(attrs.mtime);
        }
        if ((flags & IFileSystem.ATTR_PERMISSIONS) != 0) {
        	int p = attrs.permissions;
        	boolean writable =
        		(attrs.uid == user.fEffectiveUID && (p & IFileSystem.S_IWUSR) != 0) ||
        		(attrs.gid == user.fEffectiveGID && (p & IFileSystem.S_IWGRP) != 0) ||
        		(p & IFileSystem.S_IWOTH) != 0;

        	result.setAttribute(EFS.ATTRIBUTE_READ_ONLY, !writable);
        	result.setAttribute(EFS.ATTRIBUTE_OWNER_READ, (p & IFileSystem.S_IRUSR) != 0);
        	result.setAttribute(EFS.ATTRIBUTE_OWNER_WRITE, (p & IFileSystem.S_IWUSR) != 0);
        	result.setAttribute(EFS.ATTRIBUTE_OWNER_EXECUTE, (p & IFileSystem.S_IXUSR) != 0);
        	result.setAttribute(EFS.ATTRIBUTE_GROUP_READ, (p & IFileSystem.S_IRGRP) != 0);
        	result.setAttribute(EFS.ATTRIBUTE_GROUP_WRITE, (p & IFileSystem.S_IWGRP) != 0);
        	result.setAttribute(EFS.ATTRIBUTE_GROUP_EXECUTE, (p & IFileSystem.S_IXGRP) != 0);
        	result.setAttribute(EFS.ATTRIBUTE_OTHER_READ, (p & IFileSystem.S_IROTH) != 0);
        	result.setAttribute(EFS.ATTRIBUTE_OTHER_WRITE, (p & IFileSystem.S_IWOTH) != 0);
        	result.setAttribute(EFS.ATTRIBUTE_OTHER_EXECUTE, (p & IFileSystem.S_IXOTH) != 0);
        	result.setDirectory((p & IFileSystem.S_IFMT) == IFileSystem.S_IFDIR);
        }
        setResult(result);
    }

}
