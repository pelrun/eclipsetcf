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

import java.net.URI;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.remote.core.IRemoteFileManager;

public class TCFFileManager implements IRemoteFileManager {
	private final TCFConnection fConnection;

	public TCFFileManager(TCFConnection connection) {
		fConnection = connection;
	}

	@Override
	public String getDirectorySeparator() {
		return "/"; //$NON-NLS-1$
	}

	@Override
	public IFileStore getResource(String pathStr) {
		if (!pathStr.startsWith("/")) { //$NON-NLS-1$
			pathStr = fConnection.getWorkingDirectory() + "/" + pathStr; //$NON-NLS-1$
		}
		return TCFFileStore.getInstance(fConnection, pathStr, null);
	}

	@Override
	public String toPath(URI uri) {
		return uri.getPath();
	}

	@Override
	public URI toURI(IPath path) {
		try {
			return TCFEclipseFileSystem.getURIFor(fConnection, path.toString());
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public URI toURI(String path) {
		return toURI(new Path(path));
	}
}
