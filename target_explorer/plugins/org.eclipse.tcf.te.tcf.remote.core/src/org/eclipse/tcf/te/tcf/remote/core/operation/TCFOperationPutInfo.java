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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneSetStat;
import org.eclipse.tcf.services.IFileSystem.DoneStat;
import org.eclipse.tcf.services.IFileSystem.FileAttrs;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.tcf.remote.core.TCFFileStore;
import org.eclipse.tcf.te.tcf.remote.core.operation.PeerInfo.DoneGetFileSystem;

public final class TCFOperationPutInfo extends TCFFileStoreOperation<Object> {

    private final boolean fSetAttribs;
	private final boolean fSetLastModified;
	private final IFileInfo fFileInfo;

	public TCFOperationPutInfo(TCFFileStore fileStore, IFileInfo info, boolean setAttribs, boolean setLastModified) {
    	super(fileStore);
    	fFileInfo = info;
    	fSetAttribs = setAttribs;
    	fSetLastModified = setLastModified;
    }

	protected IFileInfo getFileInfo() {
	    return fFileInfo;
    }

	protected boolean isSetAttribs() {
	    return fSetAttribs;
    }

	protected boolean isSetLastModified() {
	    return fSetLastModified;
    }

	@Override
    protected void doExecute() {
    	getFileSystem(new DoneGetFileSystem() {
    		@Override
    		public void done(final IFileSystem fileSystem, IStatus status) {
    			if (shallAbort(status))
    				return;
    			stat(fileSystem, getFileStore(), new DoneStat() {
    				@Override
    				public void doneStat(IToken token, FileSystemException error, FileAttrs attrs) {
    					if (shallAbort(error))
    						return;
    					int p = attrs.permissions;
						long mtime = attrs.mtime;
						IFileInfo i = getFileInfo();
						if (isSetAttribs()) {
    						boolean ro = i.getAttribute(EFS.ATTRIBUTE_READ_ONLY);
    						p = set(p, IFileSystem.S_IRUSR, i.getAttribute(EFS.ATTRIBUTE_OWNER_READ));
    						p = set(p, IFileSystem.S_IWUSR, !ro && i.getAttribute(EFS.ATTRIBUTE_OWNER_WRITE));
    						p = set(p, IFileSystem.S_IXUSR, i.getAttribute(EFS.ATTRIBUTE_OWNER_EXECUTE));
    						p = set(p, IFileSystem.S_IRGRP, i.getAttribute(EFS.ATTRIBUTE_GROUP_READ));
    						p = set(p, IFileSystem.S_IWGRP, !ro && i.getAttribute(EFS.ATTRIBUTE_GROUP_WRITE));
    						p = set(p, IFileSystem.S_IXGRP, i.getAttribute(EFS.ATTRIBUTE_GROUP_EXECUTE));
    						p = set(p, IFileSystem.S_IROTH, i.getAttribute(EFS.ATTRIBUTE_OTHER_READ));
    						p = set(p, IFileSystem.S_IWOTH, !ro && i.getAttribute(EFS.ATTRIBUTE_OTHER_WRITE));
    						p = set(p, IFileSystem.S_IXOTH, i.getAttribute(EFS.ATTRIBUTE_OTHER_EXECUTE));
    					}
    					if (isSetLastModified()) {
    						mtime = i.getLastModified();
    					}

    					getFileStore().setAttributes(null);
						FileAttrs newAttrs = new FileAttrs(attrs.flags, attrs.size, attrs.uid, attrs.gid,
										p, attrs.atime, mtime, attrs.attributes);
    					fileSystem.setstat(getPath(), newAttrs, new DoneSetStat() {
							@Override
							public void doneSetStat(IToken token, FileSystemException error) {
		    					if (shallAbort(error))
		    						return;
		    					setResult(null);
							}
						});
    				}

					private int set(int p, int flag, boolean set) {
						return set ? p | flag : p & ~flag;
                    }
    			});
    		}
    	});
	}
}
