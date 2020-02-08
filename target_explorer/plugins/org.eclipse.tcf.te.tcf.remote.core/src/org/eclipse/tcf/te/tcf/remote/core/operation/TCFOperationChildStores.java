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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.DirEntry;
import org.eclipse.tcf.services.IFileSystem.DoneClose;
import org.eclipse.tcf.services.IFileSystem.DoneOpen;
import org.eclipse.tcf.services.IFileSystem.DoneReadDir;
import org.eclipse.tcf.services.IFileSystem.DoneRoots;
import org.eclipse.tcf.services.IFileSystem.FileSystemException;
import org.eclipse.tcf.services.IFileSystem.IFileHandle;
import org.eclipse.tcf.te.tcf.remote.core.TCFFileStore;
import org.eclipse.tcf.te.tcf.remote.core.operation.PeerInfo.DoneGetFileSystem;

public class TCFOperationChildStores extends TCFFileStoreOperation<IFileStore[]> {

	private final List<IFileStore> fFileStores = new ArrayList<IFileStore>();
    private IFileSystem fFileSystem;
	private IFileHandle fFileHandle;

	public TCFOperationChildStores(TCFFileStore filestore) {
	    super(filestore);
    }

    protected void setResult() {
    	setResult(fFileStores.toArray(new IFileStore[fFileStores.size()]));
    }

    protected final void setFileSystem(IFileSystem fileSystem) {
	    fFileSystem = fileSystem;
    }

    protected final void setFileHandle(IFileHandle fileHandle) {
	    fFileHandle = fileHandle;
    }

    @Override
    protected IFileStore[] waitForResult(SubMonitor sm) throws CoreException, InterruptedException, OperationCanceledException {
        return super.waitForResult(sm);
    }

	@Override
    protected void doExecute() {
		getFileSystem(new DoneGetFileSystem() {
			@Override
			public void done(final IFileSystem fileSystem, IStatus status) {
				if (shallAbort(status))
					return;

				setFileSystem(fileSystem);
				final String path = getPath();
				fileSystem.opendir(path, new DoneOpen() {
					@Override
					public void doneOpen(IToken token, FileSystemException error, IFileHandle handle) {
						if (error != null && (path.length() == 0 || path.equals("/"))) { //$NON-NLS-1$
							getFileStore().setIsArtificialRoot();
							readRoots();
						} else if (!shallAbort(error)) {
							setFileHandle(handle);
							readDir();
						}
					}
				});
			}
		});
	}

	protected void readRoots() {
		fFileSystem.roots(new DoneRoots() {
			@Override
			public void doneRoots(IToken token, FileSystemException error, DirEntry[] entries) {
				if (shallAbort(error)) {
					return;
				}
				for (DirEntry dirEntry : entries) {
					createFileStore(dirEntry);
				}
				setResult();
			}
		});
    }

	protected void readDir() {
		fFileSystem.readdir(fFileHandle, new DoneReadDir() {
			@Override
			public void doneReadDir(IToken token, FileSystemException error, DirEntry[] entries, boolean eof) {
				if (shallAbort(error)) {
					closeHandle();
					return;
				}

				for (DirEntry dirEntry : entries) {
					createFileStore(dirEntry);
				}
				if (eof) {
					closeHandle();
					setResult();
				} else {
					readDir();
				}
			}
		});
    }

	protected void createFileStore(DirEntry dirEntry) {
		IFileStore child = getFileStore().getChild(dirEntry.filename);
		if (child instanceof TCFFileStore) {
			((TCFFileStore) child).setAttributes(dirEntry.attrs);
		}
		fFileStores.add(child);
    }

	protected void closeHandle() {
		if (fFileSystem != null && fFileHandle != null) {
			fFileSystem.close(fFileHandle, new DoneClose() {
				@Override
				public void doneClose(IToken token, FileSystemException error) {
				}
			});
			fFileSystem = null;
			fFileHandle = null;
		}
	}
}
