/*******************************************************************************
 * Copyright (c) 2014, 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.remote.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.tcf.services.IFileSystem.FileAttrs;
import org.eclipse.tcf.te.tcf.remote.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.remote.core.operation.TCFOperationChildStores;
import org.eclipse.tcf.te.tcf.remote.core.operation.TCFOperationDelete;
import org.eclipse.tcf.te.tcf.remote.core.operation.TCFOperationFetchInfo;
import org.eclipse.tcf.te.tcf.remote.core.operation.TCFOperationMkDir;
import org.eclipse.tcf.te.tcf.remote.core.operation.TCFOperationOpenInputStream;
import org.eclipse.tcf.te.tcf.remote.core.operation.TCFOperationOpenOutputStream;
import org.eclipse.tcf.te.tcf.remote.core.operation.TCFOperationPutInfo;

public final class TCFFileStore extends FileStore {

	public static IFileStore getInstance(URI uri) {
		Path path = new Path(uri.getPath());
		IRemoteConnection connection = TCFEclipseFileSystem.getConnection(uri);
		if (connection instanceof TCFConnection)
			return new TCFFileStore((TCFConnection) connection, path, uri, null);

		return EFS.getNullFileSystem().getStore(path);
	}

	public static IFileStore getInstance(TCFConnection connection, IPath path, TCFFileStore parent) {
		try {
	        return new TCFFileStore(connection, path, TCFEclipseFileSystem.getURIFor(connection, path.toString()), parent);
        } catch (URISyntaxException e) {
        	Platform.getLog(CoreBundleActivator.getContext().getBundle()).log(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), Messages.TCFFileManager_errorFileStoreForPath, e));
        }
		return EFS.getNullFileSystem().getStore(path);
	}

	private final URI fURI;
	private final TCFConnection fConnection;
	private final IPath fRemotePath;
	private FileAttrs fAttributes;
	private IFileStore fParent;

	private TCFFileStore(TCFConnection connection, IPath path, URI uri, TCFFileStore parent) {
		fURI = uri;
		fConnection = connection;
		fRemotePath = new Path(uri.getPath());
		fParent = parent;
	}

	@Override
	public URI toURI() {
		return fURI;
	}

	public TCFConnection getConnection() {
	    return fConnection;
	}

	public IPath getPath() {
		return fRemotePath;
	}

	public void setAttributes(FileAttrs attrs) {
		fAttributes = attrs;
	}

	public FileAttrs getAttributes() {
	    return fAttributes;
	}

	@Override
	public IFileStore getChild(String name) {
		return getInstance(fConnection, fRemotePath.append(name), this);
	}

	@Override
	public String getName() {
		if (fRemotePath.isRoot()) {
			return fRemotePath.toString();
		}
		return fRemotePath.lastSegment();
	}

	@Override
	public IFileStore getParent() {
		if (fParent != null)
			return fParent;

		if (fRemotePath.isRoot())
			return null;

		fParent = getInstance(fConnection, fRemotePath.removeLastSegments(1), null);
		return fParent;
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
		try {
	        return new TCFOperationFetchInfo(this).execute(SubMonitor.convert(monitor));
        } catch (OperationCanceledException e) {
        }
    	return new FileInfo(getName());
	}

	@Override
	public IFileStore[] childStores(int options, IProgressMonitor monitor) throws CoreException {
		try {
			return new TCFOperationChildStores(this).execute(SubMonitor.convert(monitor));
        } catch (OperationCanceledException e) {
        }
    	return new IFileStore[0];
	}

	@Override
	public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
		IFileStore[] children = childStores(options, monitor);
		String[] result = new String[children.length];
		int i = 0;
		for (IFileStore s : children) {
			result[i++] = s.getName();
		}
		return result;
	}

	@Override
	public void delete(int options, IProgressMonitor monitor) throws CoreException {
		try {
	        new TCFOperationDelete(this).execute(SubMonitor.convert(monitor));
        } catch (OperationCanceledException e) {
        }
	}

	@Override
	public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
		boolean shallow = (options & EFS.SHALLOW) == EFS.SHALLOW;
		try {
			new TCFOperationMkDir(this, shallow).execute(SubMonitor.convert(monitor));
        } catch (OperationCanceledException e) {
        }
		return this;
	}

	@Override
	public void putInfo(IFileInfo info, int options, IProgressMonitor monitor) throws CoreException {
		boolean setAttribs = (options & EFS.SET_ATTRIBUTES) != 0;
		boolean setLastModified = (options & EFS.SET_LAST_MODIFIED) != 0;
		try {
			if (setAttribs || setLastModified) {
				new TCFOperationPutInfo(this, info, setAttribs, setLastModified).execute(SubMonitor.convert(monitor));
			}
        } catch (OperationCanceledException e) {
        }
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		try {
	        return new TCFOperationOpenInputStream(this).execute(SubMonitor.convert(monitor));
        } catch (OperationCanceledException e) {
        	return null;
        }
	}

	@Override
	public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
		boolean append = (options & EFS.APPEND) != 0;
		try {
	        return new TCFOperationOpenOutputStream(this, append).execute(SubMonitor.convert(monitor));
        } catch (OperationCanceledException e) {
        	return null;
        }
	}
}
