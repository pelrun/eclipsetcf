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
package org.eclipse.tcf.te.tcf.remote.ui;

import java.net.URI;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.remote.core.IRemoteConnectionType;
import org.eclipse.remote.core.IRemotePreferenceConstants;
import org.eclipse.remote.core.IRemoteServicesManager;
import org.eclipse.remote.internal.core.RemotePath;
import org.eclipse.remote.ui.IRemoteUIFileService;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.tcf.remote.core.TCFConnection;
import org.eclipse.tcf.te.tcf.remote.core.TCFConnectionManager;
import org.eclipse.tcf.te.tcf.remote.core.TCFFileStore;
import org.eclipse.tcf.te.tcf.remote.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.remote.ui.nls.Messages;
import org.eclipse.ui.ide.fileSystem.FileSystemContributor;

public class TCFFileSystemContributor extends FileSystemContributor {
	private static final String REMOTE_CORE_PLUGIN_ID = "org.eclipse.remote.core"; //$NON-NLS-1$

	@Override
	public URI browseFileSystem(String initialPath, Shell shell) {
		IRemoteServicesManager manager = UIPlugin.getService(IRemoteServicesManager.class);
		if (manager == null)
			return null;
		IRemoteConnectionType connectionType = manager.getConnectionType(TCFConnection.CONNECTION_TYPE_ID);
		if (connectionType == null)
			return null;

		IRemoteUIFileService uiFileMgr = connectionType.getService(IRemoteUIFileService.class);

		uiFileMgr.showConnections(true);
		String original = setPreferredService(TCFConnection.CONNECTION_TYPE_ID);
		try {
			String path = uiFileMgr.browseDirectory(shell, Messages.TCFFileSystemContributor_browseFileSystem_title, initialPath, 0);
			if (path != null) {
				path = workaroundBug472329(path);
				TCFConnection conn = TCFConnectionManager.INSTANCE.mapConnection(uiFileMgr.getConnection());
				if (conn != null) {
					return TCFFileStore.toURI(conn, path);
				}
			}
		} finally {
			setPreferredService(original);
		}
		return null;
	}

	private String workaroundBug472329(String path) {
		return TCFFileStore.stripNoSlashMarker(path);
	}

	private String setPreferredService(String id) {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(REMOTE_CORE_PLUGIN_ID);
		String key = IRemotePreferenceConstants.PREF_CONNECTION_TYPE_ID;
		String old = node.get(key, null);
		if (id == null) {
			node.remove(key);
		} else {
			node.put(key, id);
		}
		return old;
    }

	@Override
	public URI getURI(String string) {
		return URIUtil.toURI(RemotePath.forPosix(string).toString());
	}
}