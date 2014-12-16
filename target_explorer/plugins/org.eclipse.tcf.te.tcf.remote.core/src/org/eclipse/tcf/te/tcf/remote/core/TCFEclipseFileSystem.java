/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.remote.core;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.IPath;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.IRemoteServices;
import org.eclipse.remote.core.RemoteServices;

public class TCFEclipseFileSystem extends FileSystem {

	public static final String SCHEME = "tcf"; //$NON-NLS-1$

	public static String getConnectionNameFor(URI uri) {
		return uri.getAuthority();
	}

	public static URI getURIFor(TCFConnection connection, String path) throws URISyntaxException {
		return new URI(SCHEME, connection.getName(), path, null, null);
	}

	public static IRemoteConnection getConnection(URI uri) {
		if (!SCHEME.equals(uri.getScheme()))
			return null;
		String peerName = uri.getAuthority();
		if (peerName == null)
			return null;

		IRemoteServices trs = RemoteServices.getRemoteServices(TCFRemoteServices.TCF_ID);
		if (trs == null)
			return null;

		return trs.getConnectionManager().getConnection(peerName);
	}

	public TCFEclipseFileSystem() {
		super();
	}

	@Override
	public int attributes() {
		return EFS.ATTRIBUTE_READ_ONLY | EFS.ATTRIBUTE_EXECUTABLE;
	}

	@Override
	public boolean canDelete() {
		return true;
	}

	@Override
	public boolean canWrite() {
		return true;
	}

	@Override
	public IFileStore getStore(IPath path) {
		return EFS.getNullFileSystem().getStore(path);
	}

	@Override
	public IFileStore getStore(URI uri) {
		return TCFFileStore.getInstance(uri);
	}
}