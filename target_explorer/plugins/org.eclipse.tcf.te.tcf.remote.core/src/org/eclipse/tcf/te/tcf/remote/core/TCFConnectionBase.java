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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.exception.RemoteConnectionException;
import org.eclipse.remote.core.exception.UnableToForwardPortException;

public abstract class TCFConnectionBase implements IRemoteConnection {

	private final TCFRemoteServices fRemoteServices;

	private String fWorkingDirectory = "/"; //$NON-NLS-1$

	public TCFConnectionBase(TCFRemoteServices tcfServices) {
		fRemoteServices = tcfServices;
    }

	@Override
	public final int compareTo(IRemoteConnection o) {
		return getName().compareTo(o.getName());
	}

	@Override
	public final String getProperty(String key) {
		return getAttributes().get(key);
	}

	@Override
	public final String getEnv(String name) {
		return getEnv().get(name);
	}

	@Override
	public final TCFRemoteServices getRemoteServices() {
		return fRemoteServices;
	}

	@Override
	public final String getWorkingDirectory() {
		return fWorkingDirectory;
	}

	@Override
	public final void forwardLocalPort(int localPort, String fwdAddress, int fwdPort) throws RemoteConnectionException {
		throw new UnableToForwardPortException(Messages.TCFConnectionBase_errorNoPortForwarding);
	}

	@Override
	public final int forwardLocalPort(String fwdAddress, int fwdPort, IProgressMonitor monitor) throws RemoteConnectionException {
		throw new UnableToForwardPortException(Messages.TCFConnectionBase_errorNoPortForwarding);
	}

	@Override
	public final void forwardRemotePort(int remotePort, String fwdAddress, int fwdPort) throws RemoteConnectionException {
		throw new UnableToForwardPortException(Messages.TCFConnectionBase_errorNoPortForwarding);
	}

	@Override
	public final int forwardRemotePort(String fwdAddress, int fwdPort, IProgressMonitor monitor) throws RemoteConnectionException {
		throw new UnableToForwardPortException(Messages.TCFConnectionBase_errorNoPortForwarding);
	}

	@Override
	public final void removeLocalPortForwarding(int port) {
	}

	@Override
	public final void removeRemotePortForwarding(int port) {
	}

	@Override
	public final void setWorkingDirectory(String path) {
		if (new Path(path).isAbsolute()) {
			fWorkingDirectory = path;
		}
	}
	@Override
	public final boolean supportsTCPPortForwarding() {
		return false;
	}

	@Override
	public final String toString() {
		String str = getName() + " [" + getAddress(); //$NON-NLS-1$
		int port = getPort();
		if (port >= 0 && port != 1756) {
			str += ":" + port; //$NON-NLS-1$
		}
		return str + "]"; //$NON-NLS-1$
	}
}
