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
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.IRemoteConnectionControlService;
import org.eclipse.remote.core.IRemoteConnectionHostService;
import org.eclipse.remote.core.IRemoteConnectionPropertyService;
import org.eclipse.remote.core.IRemoteFileService;
import org.eclipse.remote.core.IRemoteProcessBuilder;
import org.eclipse.remote.core.IRemoteProcessService;
import org.eclipse.remote.core.RemoteConnectionChangeEvent;
import org.eclipse.remote.core.exception.RemoteConnectionException;
import org.eclipse.remote.internal.core.RemotePath;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.remote.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.remote.core.operation.TCFOperationGetEnvironment;

public class TCFConnection implements
		IRemoteConnectionHostService,
		IRemoteConnectionPropertyService,
		IRemoteProcessService,
		IRemoteConnectionControlService,
		IRemoteFileService {

	public static final String CONNECTION_TYPE_ID = "org.eclipse.tcf.te.tcf.remote"; //$NON-NLS-1$

	private IRemoteConnection fRemoteConnection;
    private volatile IPeerNode fPeerNode;

	private volatile boolean fOpen;
	private Map<String, String> fEnvironment;
	private String fWorkingDirectory = "/"; //$NON-NLS-1$
	private String fBaseDirectory = ""; //$NON-NLS-1$

	public TCFConnection(IPeerNode peerNode) {
    	fPeerNode = peerNode;
    	fOpen = peerNode.getConnectState() == IConnectable.STATE_CONNECTED;
    }

	public TCFConnection(IRemoteConnection rc) {
		fRemoteConnection = rc;
		fOpen = false;
	}

	public String getName() {
		if (fPeerNode != null)
			return fPeerNode.getName();
		return fRemoteConnection.getName();
	}

	@Override
	public IRemoteConnection getRemoteConnection() {
		return fRemoteConnection;
	}

	void setRemoteConnection(IRemoteConnection remoteConnection) {
		fRemoteConnection = remoteConnection;
	}

	void setPeerNode(IPeerNode peerNode) {
		if (fPeerNode == peerNode)
			return;

		fPeerNode = peerNode;
		setConnectedTCF(peerNode.getConnectState() == IConnectable.STATE_CONNECTED);
	}

	public IPeerNode getPeerNode() {
	    return fPeerNode;
	}

	// IRemoteConnectionHostService
	@Override
	public String getHostname() {
		if (fPeerNode == null)
			return "0.0.0.0"; //$NON-NLS-1$

		return getPeerProperty(IPeer.ATTR_IP_HOST);
	}

	private String getPeerProperty(String key) {
		return fPeerNode.getPeer().getAttributes().get(key);
	}

	@Override
	public int getPort() {
		if (fPeerNode != null) {
			try {
				return Integer.parseInt(getPeerProperty(IPeer.ATTR_IP_PORT));
			} catch (Exception e) {
			}
		}
		return 0;
	}

	@Override
	public int getTimeout() {
		return 60;
	}

	@Override
	public boolean useLoginShell() {
		return false;
	}

	@Override
	public String getUsername() {
		if (fPeerNode == null)
			return ""; //$NON-NLS-1$
		return getPeerProperty(IPeer.ATTR_USER_NAME);
	}

	@Override
	public void setHostname(String hostname) {}

	@Override
	public void setPassphrase(String passphrase) {}

	@Override
	public void setPassword(String password) {}

	@Override
	public void setPort(int port) {}

	@Override
	public void setTimeout(int timeout) {}

	@Override
	public void setUseLoginShell(boolean useLogingShell) {}

	@Override
	public void setUsePassword(boolean usePassword) {}

	@Override
	public void setUsername(String username) {}

	// IRemoteConnectionPropertyService
	@Override
	public String getProperty(String key) {
		if (fPeerNode == null)
			return null;

		if (IRemoteConnection.OS_NAME_PROPERTY.equals(key)) {
			return getPeerProperty(IPeer.ATTR_OS_NAME);
		}

		return null;
	}

	// IRemoteProcessService
	@Override
	public Map<String, String> getEnv() {
		if (fEnvironment == null && fPeerNode != null) {
			try {
	            fEnvironment = new TCFOperationGetEnvironment(fPeerNode.getPeer()).execute(SubMonitor.convert(null));
            } catch (OperationCanceledException e) {
            } catch (CoreException e) {
            	Platform.getLog(CoreBundleActivator.getContext().getBundle()).log(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), Messages.TCFConnection_errorNoEnvironment, e));
            }
		}
		return fEnvironment;
	}

	@Override
	public String getEnv(String name) {
		Map<String, String> map = getEnv();
		if (map != null)
			return map.get(name);
		return null;
	}

	@Override
	public IRemoteProcessBuilder getProcessBuilder(List<String> command) {
		if (!isOpen())
			return null;

		return new TCFProcessBuilder(this, command);
	}

	@Override
	public IRemoteProcessBuilder getProcessBuilder(String... command) {
		if (!isOpen())
			return null;

		return new TCFProcessBuilder(this, command);
	}


	@Override
	public String getWorkingDirectory() {
		return fWorkingDirectory;
	}

	@Override
	public void setWorkingDirectory(String path) {
		fWorkingDirectory = path;
	}

    // IRemoteConnectionControlService
	@Override
	public boolean isOpen() {
		return fOpen;
	}

	@Override
	public void open(IProgressMonitor monitor) throws RemoteConnectionException {
		if (fPeerNode == null) {
			TCFConnectionManager.INSTANCE.waitForInitialization(monitor);
		}
		boolean open = false;
		synchronized (this) {
			if (fOpen)
				return;

			if (fPeerNode.getConnectState() == IConnectable.STATE_CONNECTED) {
				fOpen = open = true;
			}
		}
		if (open) {
			fireConnectionChangeEvent(RemoteConnectionChangeEvent.CONNECTION_OPENED);
		} else {
			TCFConnectionManager.INSTANCE.open(fPeerNode, monitor);
		}
	}

	@Override
	public void close() {
		synchronized (this) {
			if (!fOpen)
				return;
			fOpen = false;
		}
		fireConnectionChangeEvent(RemoteConnectionChangeEvent.CONNECTION_CLOSED);
		TCFConnectionManager.INSTANCE.close(fPeerNode);
	}

	void setConnectedTCF(boolean connected) {
		int notify = -1;
		synchronized (this) {
			if (connected) {
				if (!fOpen) {
					fOpen = true;
					notify = RemoteConnectionChangeEvent.CONNECTION_OPENED;
				}
			} else {
				if (fOpen) {
					fOpen = false;
					notify = RemoteConnectionChangeEvent.CONNECTION_CLOSED;
				}
			}
		}
		if (notify != -1) {
			fireConnectionChangeEvent(notify);
		}
    }

	private void fireConnectionChangeEvent(final int type) {
		final IRemoteConnection rc = fRemoteConnection;
		if (rc == null)
			return;

		new Job(Messages.TCFConnection_notifyListeners) {
            @Override
			protected IStatus run(IProgressMonitor monitor) {
				rc.fireConnectionChangeEvent(type);
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	// IRemoteFileService
	@Override
	public String getBaseDirectory() {
		return fBaseDirectory;
	}

	@Override
	public String getDirectorySeparator() {
		return "/"; //$NON-NLS-1$
	}

	@Override
	public IFileStore getResource(String path) {
		return new TCFFileStore(this, RemotePath.forPosix(path).toString(), null);
	}

	@Override
	public void setBaseDirectory(String path) {
		fBaseDirectory = path;
	}

	@Override
	public String toPath(URI uri) {
		return TCFFileStore.toPath(uri);
	}

	@Override
	public URI toURI(String path) {
		return toURI(RemotePath.forPosix(path));
	}

	@Override
	public URI toURI(IPath path) {
		if (!path.isAbsolute() && fBaseDirectory != null && fBaseDirectory.length() > 0) {
			path = RemotePath.forPosix(fBaseDirectory).append(path);
		}
		return TCFFileStore.toURI(this, path.toString());
	}

	public String getPassphrase() {
		return null;
	}

	public String getPassword() {
		return null;
	}

	public boolean usePassword() {
		return false;
	}
}
