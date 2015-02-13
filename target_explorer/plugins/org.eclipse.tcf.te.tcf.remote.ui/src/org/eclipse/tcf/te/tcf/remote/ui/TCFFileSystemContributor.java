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
package org.eclipse.tcf.te.tcf.remote.ui;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.util.NLS;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.IRemotePreferenceConstants;
import org.eclipse.remote.core.IRemoteServices;
import org.eclipse.remote.core.RemoteServices;
import org.eclipse.remote.ui.IRemoteUIFileManager;
import org.eclipse.remote.ui.IRemoteUIServices;
import org.eclipse.remote.ui.RemoteUIServices;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.tcf.remote.core.TCFConnection;
import org.eclipse.tcf.te.tcf.remote.core.TCFEclipseFileSystem;
import org.eclipse.tcf.te.tcf.remote.core.TCFRemoteServices;
import org.eclipse.tcf.te.tcf.remote.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.remote.ui.nls.Messages;
import org.eclipse.ui.ide.fileSystem.FileSystemContributor;

public class TCFFileSystemContributor extends FileSystemContributor {
	private static final String REMOTE_CORE_PLUGIN_ID = "org.eclipse.remote.core"; //$NON-NLS-1$

	@Override
	public URI browseFileSystem(String initialPath, Shell shell) {
		IRemoteServices services = RemoteServices.getRemoteServices(TCFRemoteServices.TCF_ID);
		IRemoteUIServices uiServices = RemoteUIServices.getRemoteUIServices(services);
		IRemoteUIFileManager uiFileMgr = uiServices.getUIFileManager();
		uiFileMgr.showConnections(true);
		String original = setPreferredService(TCFRemoteServices.TCF_ID);
		try {
			String path = uiFileMgr.browseDirectory(shell, Messages.TCFFileSystemContributor_browseFileSystem_title, initialPath, 0);
			if (path != null) {
				IRemoteConnection conn = uiFileMgr.getConnection();
				if (conn instanceof TCFConnection) {
					TCFConnection tcfConn = (TCFConnection) conn;
					try {
						return TCFEclipseFileSystem.getURIFor(tcfConn, path);
					} catch (URISyntaxException e) {
						Platform.getLog(UIPlugin.getDefault().getBundle()).log(
										new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(), NLS.bind(Messages.TCFFileSystemContributor_errorCreateURIForPath, conn.getName(), path), e));
					}
				}
			}
		} finally {
			setPreferredService(original);
		}
		return null;
	}

	private String setPreferredService(String id) {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(REMOTE_CORE_PLUGIN_ID);
		String key = IRemotePreferenceConstants.PREF_REMOTE_SERVICES_ID;
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
		try {
			return new URI(string);
		} catch (URISyntaxException e) {
			// Ignore
		}
		return null;
	}
}