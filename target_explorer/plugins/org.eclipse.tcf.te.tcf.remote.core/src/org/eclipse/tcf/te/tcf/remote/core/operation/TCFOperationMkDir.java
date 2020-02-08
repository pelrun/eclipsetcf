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

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneMkDir;
import org.eclipse.tcf.services.IFileSystem.DoneStat;
import org.eclipse.tcf.services.IFileSystem.FileAttrs;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.tcf.remote.core.TCFFileStore;
import org.eclipse.tcf.te.tcf.remote.core.operation.PeerInfo.DoneGetFileSystem;

public final class TCFOperationMkDir extends TCFFileStoreOperation<Object> {

    protected final boolean fShallow;

	public TCFOperationMkDir(TCFFileStore filestore, boolean shallow) {
	    super(filestore);
	    fShallow = shallow;
    }

    @Override
    protected void doExecute() {
    	getFileSystem(new DoneGetFileSystem() {
    		@Override
    		public void done(final IFileSystem fileSystem, IStatus status) {
    			if (shallAbort(status))
    				return;

    			DoneMkDir callback = new DoneMkDir() {
    				@Override
    				public void doneMkDir(IToken token, FileSystemException error) {
    					if (shallAbort(error))
    						return;
    					setResult(null);
    				}
    			};
    			mkdir(fileSystem, getFileStore(), callback);
    		}
    	});
	}

    protected void mkdir(final IFileSystem fs, final TCFFileStore fileStore, final DoneMkDir callback) {
    	stat(fs, fileStore, new DoneStat() {
    		@Override
    		public void doneStat(IToken token, FileSystemException error, FileAttrs attrs) {
    			if (error == null) {
    				fileStore.setAttributes(attrs);
    			}
    			if (error == null && attrs.isDirectory()) {
    				// Directory exists, ok.
    				callback.doneMkDir(token, null);
    			} else if (error != null && error.getStatus() != IFileSystem.STATUS_NO_SUCH_FILE) {
    				// Error and file exists
    				callback.doneMkDir(token, error);
    			} else {
    				// Directory does not exist
    				final IFileStore parent = fShallow ? null : fileStore.getParent();
    				if (parent instanceof TCFFileStore) {
    					mkdir(fs, (TCFFileStore) parent, new DoneMkDir() {
							@Override
							public void doneMkDir(IToken token, FileSystemException error) {
								if (error != null) {
									callback.doneMkDir(token, error);
								} else {
			    					fs.mkdir(fileStore.getPath(), null, callback);
								}
							}
						});
    			    } else {
    					fs.mkdir(fileStore.getPath(), null, callback);
    				}
    			}
    		}
    	});
    }
}
