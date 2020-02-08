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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneRemove;
import org.eclipse.tcf.services.IFileSystem.DoneStat;
import org.eclipse.tcf.services.IFileSystem.FileAttrs;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.te.tcf.remote.core.TCFFileStore;
import org.eclipse.tcf.te.tcf.remote.core.operation.PeerInfo.DoneGetFileSystem;

public final class TCFOperationDelete extends TCFFileStoreOperation<Object> {

    public TCFOperationDelete(TCFFileStore filestore) {
	    super(filestore);
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
    					DoneRemove callback = new DoneRemove() {
    						@Override
    						public void doneRemove(IToken token, FileSystemException error) {
    							if (shallAbort(error))
    								return;
    							setResult(null);
    						}
    					};
    					getFileStore().setAttributes(null);
    					if (attrs.isDirectory()) {
    						fileSystem.rmdir(getPath(), callback);
    					} else {
    						fileSystem.remove(getPath(), callback);
    					}
    				}
    			});
    		}
    	});
	}
}
