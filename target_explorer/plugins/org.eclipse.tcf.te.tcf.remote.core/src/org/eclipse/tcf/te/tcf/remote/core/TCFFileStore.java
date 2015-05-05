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
		IRemoteConnection connection = TCFEclipseFileSystem.getConnection(uri);
		if (connection instanceof TCFConnection)
			return new TCFFileStore((TCFConnection) connection, uri, null);

		return EFS.getNullFileSystem().getStore(new Path(uri.getPath()));
	}

	public static IFileStore getInstance(TCFConnection connection, String path, TCFFileStore parent) {
		try {
	        URI uri = TCFEclipseFileSystem.getURIFor(connection, path);
			return new TCFFileStore(connection, uri, parent);
        } catch (URISyntaxException e) {
        	Platform.getLog(CoreBundleActivator.getContext().getBundle()).log(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), Messages.TCFFileManager_errorFileStoreForPath, e));
        }
		return EFS.getNullFileSystem().getStore(new Path(path));
	}

	private final URI fURI;
	private final TCFConnection fConnection;
	private FileAttrs fAttributes;
	private IFileStore fParent;

	private TCFFileStore(TCFConnection connection, URI uri, TCFFileStore parent) {
		fURI = uri;
		fConnection = connection;
		fParent = parent;
	}

	@Override
	public URI toURI() {
		return fURI;
	}

	public TCFConnection getConnection() {
	    return fConnection;
	}

	public String getPath() {
		return fURI.getPath();
	}

	public void setAttributes(FileAttrs attrs) {
		fAttributes = attrs;
	}

	public FileAttrs getAttributes() {
	    return fAttributes;
	}

	@Override
	public IFileStore getChild(String name) {
		String path = getPath() + '/' + name;
		path = path.replaceAll("/+", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		if (path.length() > 1 && path.endsWith("/")) //$NON-NLS-1$
			path = path.substring(0, path.length()-1);
		return getInstance(fConnection, path, this);
	}

	@Override
	public String getName() {
		String path = getPath();
		int idx = path.lastIndexOf('/');
		if (idx > 0)
			return path.substring(idx + 1);
		return path;
	}

	@Override
	public IFileStore getParent() {
		if (fParent != null)
			return fParent;

		String path = getPath();
		int idx = path.lastIndexOf('/');
		if (idx < 1)
			return null;

		fParent = getInstance(fConnection, path.substring(0, idx-1), null);
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
