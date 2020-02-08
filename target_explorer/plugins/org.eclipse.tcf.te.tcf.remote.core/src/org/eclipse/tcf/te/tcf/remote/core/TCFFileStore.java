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
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.remote.core.IRemoteConnectionType;
import org.eclipse.remote.core.IRemoteServicesManager;
import org.eclipse.remote.internal.core.RemotePath;
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
	public static final String SCHEME = "tcf"; //$NON-NLS-1$
	public static final String NOSLASH_MARKER = "/./"; //$NON-NLS-1$

	public static String addNoSlashMarker(String path) {
		if (path.length() > 0 && path.charAt(0) != '/')
			return NOSLASH_MARKER + path;
		return path;
	}

	public static String stripNoSlashMarker(String path) {
		if (path.startsWith(NOSLASH_MARKER))
			return path.substring(NOSLASH_MARKER.length());
		return path;
	}

	public static URI toURI(TCFConnection connection, String path) {
		try {
			return new URI(SCHEME, connection.getName(), addNoSlashMarker(path), null, null);
		} catch (URISyntaxException e) {
			return null;
		}
	}

	public static String toPath(URI uri) {
		return stripNoSlashMarker(uri.getPath());
	}


	public static TCFConnection toConnection(URI uri) {
		IRemoteServicesManager rsm = CoreBundleActivator.getService(IRemoteServicesManager.class);
		if (rsm == null)
			return null;

		IRemoteConnectionType ct = rsm.getConnectionType(uri);
		if (ct == null)
			return null;

		String peerName = uri.getAuthority();
		if (peerName == null)
			return null;

		return TCFConnectionManager.INSTANCE.mapConnection(ct.getConnection(peerName));
	}


	public static IFileStore getInstance(URI uri) {
		TCFConnection connection = toConnection(uri);
		String path = toPath(uri);
		if (connection != null) {
			return new TCFFileStore(connection, path, null);
		}
		return EFS.getNullFileSystem().getStore(RemotePath.forPosix(path));
	}

	private final TCFConnection fConnection;
	private final String fPath;
	private FileAttrs fAttributes;
	private TCFFileStore fParent;
	private boolean fArtificialRoot;

	TCFFileStore(TCFConnection connection, String path, TCFFileStore parent) {
		fPath = path;
		fConnection = connection;
		fParent = parent;
	}

	@Override
	public URI toURI() {
		return toURI(fConnection, fPath);
	}

	public TCFConnection getConnection() {
	    return fConnection;
	}

	public String getPath() {
		return fPath;
	}

	public void setAttributes(FileAttrs attrs) {
		fAttributes = attrs;
	}

	public FileAttrs getAttributes() {
	    return fAttributes;
	}

	@Override
	public IFileStore getChild(String name) {
		// Remove trailing slash
		if (name.length() > 1 && name.endsWith("/")) //$NON-NLS-1$
			name = name.substring(0, name.length()-1);

		String path;
		if (fArtificialRoot) {
			path = name;
		} else if (fPath.endsWith("/")) { //$NON-NLS-1$
			path = fPath + name;
		} else {
			path = fPath + "/" + name; //$NON-NLS-1$
		}
		return new TCFFileStore(fConnection, path, this);
	}

	@Override
	public String getName() {
		String path = getPath();
		if (fParent == null || fParent.fArtificialRoot)
			return path;

		int idx = path.lastIndexOf('/', path.length()-2);
		if (idx > 0)
			return path.substring(idx + 1);
		return path;
	}

	@Override
	public IFileStore getParent() {
		if (fParent != null)
			return fParent;

		int idx = fPath.lastIndexOf('/');
		if (idx < 1)
			return null;

		fParent = new TCFFileStore(fConnection, fPath.substring(0, idx-1), null);
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

	public void setIsArtificialRoot() {
		fArtificialRoot = true;
	}
}
