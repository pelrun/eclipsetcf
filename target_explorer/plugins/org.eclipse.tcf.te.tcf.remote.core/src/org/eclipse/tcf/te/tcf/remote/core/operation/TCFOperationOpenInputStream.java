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

import java.io.InputStream;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DoneOpen;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.services.IFileSystem.IFileHandle;
import org.eclipse.tcf.te.tcf.remote.core.TCFFileStore;
import org.eclipse.tcf.te.tcf.remote.core.operation.PeerInfo.DoneGetFileSystem;
import org.eclipse.tcf.util.TCFFileInputStream;

public class TCFOperationOpenInputStream extends TCFFileStoreOperation<InputStream> {

    public TCFOperationOpenInputStream(TCFFileStore filestore) {
	    super(filestore);
    }

	@Override
	protected void doExecute() {
    	getFileSystem(new DoneGetFileSystem() {
    		@Override
    		public void done(final IFileSystem fileSystem, IStatus status) {
    			if (shallAbort(status))
    				return;

    			fileSystem.open(getPath(), IFileSystem.TCF_O_READ, null, new DoneOpen() {
					@SuppressWarnings("resource")
                    @Override
                    public void doneOpen(IToken token, FileSystemException error, IFileHandle handle) {
						if (shallAbort(error))
							return;
						setResult(new TCFFileInputStream(handle));
                    }
    			});
    		}
    	});
	}
}
