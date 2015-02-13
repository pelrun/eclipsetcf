/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 *     Markus Schorn - Adapted for TCF remote service
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.remote.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.window.Window;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.ui.IRemoteUIFileManager;
import org.eclipse.remote.ui.dialogs.RemoteResourceBrowser;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

public class TCFUIFileManager implements IRemoteUIFileManager {
	private IRemoteConnection fConnection = null;
	private boolean fShowConnections = false;

	@Override
	public String browseDirectory(Shell shell, String message, String filterPath, int flags) {
		RemoteResourceBrowser browser = new RemoteResourceBrowser(shell, SWT.SINGLE);
		browser.setType(RemoteResourceBrowser.DIRECTORY_BROWSER);
		browser.setInitialPath(filterPath);
		browser.setTitle(message);
		browser.showConnections(fShowConnections);
		browser.setConnection(fConnection);
		if (browser.open() == Window.CANCEL) {
			return null;
		}
		fConnection = browser.getConnection();
		IFileStore resource = browser.getResource();
		if (resource == null) {
			return null;
		}
		return resource.toURI().getPath();
	}

	@Override
	public String browseFile(Shell shell, String message, String filterPath, int flags) {
		RemoteResourceBrowser browser = new RemoteResourceBrowser(shell, SWT.SINGLE);
		browser.setType(RemoteResourceBrowser.FILE_BROWSER);
		browser.setInitialPath(filterPath);
		browser.setTitle(message);
		browser.showConnections(fShowConnections);
		browser.setConnection(fConnection);
		if (browser.open() == Window.CANCEL) {
			return null;
		}
		fConnection = browser.getConnection();
		IFileStore resource = browser.getResource();
		if (resource == null) {
			return null;
		}
		return resource.toURI().getPath();
	}

	@Override
	public List<String> browseFiles(Shell shell, String message, String filterPath, int flags) {
		RemoteResourceBrowser browser = new RemoteResourceBrowser(shell, SWT.MULTI);
		browser.setType(RemoteResourceBrowser.FILE_BROWSER);
		browser.setInitialPath(filterPath);
		browser.setTitle(message);
		browser.showConnections(fShowConnections);
		browser.setConnection(fConnection);
		if (browser.open() == Window.CANCEL) {
			return null;
		}
		fConnection = browser.getConnection();
		List<String> paths = new ArrayList<String>();
		for (IFileStore store : browser.getResources()) {
			paths.add(store.toURI().getPath());
		}
		return paths;
	}

	@Override
	public IRemoteConnection getConnection() {
		return fConnection;
	}

	@Override
	public void setConnection(IRemoteConnection connection) {
		this.fConnection = connection;
	}

	@Override
	public void showConnections(boolean enable) {
		fShowConnections = enable;
	}
}
