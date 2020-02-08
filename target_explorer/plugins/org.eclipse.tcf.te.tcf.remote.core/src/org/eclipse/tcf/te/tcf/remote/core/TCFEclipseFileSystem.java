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

import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.IPath;

public class TCFEclipseFileSystem extends FileSystem {


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