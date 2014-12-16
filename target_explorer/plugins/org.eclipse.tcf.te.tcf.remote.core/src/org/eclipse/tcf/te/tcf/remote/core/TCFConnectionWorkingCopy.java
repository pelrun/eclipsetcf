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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.IRemoteConnectionChangeListener;
import org.eclipse.remote.core.IRemoteConnectionWorkingCopy;
import org.eclipse.remote.core.IRemoteFileManager;
import org.eclipse.remote.core.IRemoteProcess;
import org.eclipse.remote.core.IRemoteProcessBuilder;
import org.eclipse.remote.core.exception.RemoteConnectionException;

public class TCFConnectionWorkingCopy extends TCFConnectionBase implements IRemoteConnectionWorkingCopy {

	private final TCFConnection fConnection;

	public TCFConnectionWorkingCopy(TCFConnection connection) {
		super(connection.getRemoteServices());
		fConnection = connection;
	}

	@Override
	public IRemoteConnection getOriginal() {
		return fConnection;
	}
	@Override
	public boolean isDirty() {
		return false;
	}
	@Override
	public IRemoteConnection save() {
		return fConnection;
	}
	@Override
	public void setAddress(String address) {
	}

	@Override
	public void setAttribute(String key, String value) {
	}

	@Override
	public void setName(String name) {
	}

	@Override
	public void setPassword(String password) {
	}

	@Override
	public void setPort(int port) {
	}

	@Override
	public void setUsername(String username) {
	}

	@Override
    public void addConnectionChangeListener(IRemoteConnectionChangeListener listener) {
		fConnection.addConnectionChangeListener(listener);
    }

	@Override
    public void close() {
		fConnection.close();
    }

	@Override
    public void fireConnectionChangeEvent(int type) {
		fConnection.fireConnectionChangeEvent(type);
    }

	@Override
    public String getAddress() {
	    return fConnection.getAddress();
    }

	@Override
    public Map<String, String> getAttributes() {
	    return fConnection.getAttributes();
    }

	@Override
    public IRemoteProcess getCommandShell(int flags) throws IOException {
		return fConnection.getCommandShell(flags);
    }

	@Override
    public Map<String, String> getEnv() {
	    return fConnection.getEnv();
    }

	@Override
    public IRemoteFileManager getFileManager() {
	    return fConnection.getFileManager();
    }

	@Override
    public String getName() {
	    return fConnection.getName();
    }

	@Override
    public int getPort() {
	    return fConnection.getPort();
    }

	@Override
    public IRemoteProcessBuilder getProcessBuilder(List<String> command) {
	    return fConnection.getProcessBuilder(command);
    }

	@Override
    public IRemoteProcessBuilder getProcessBuilder(String... command) {
	    return fConnection.getProcessBuilder(command);
    }

	@Override
    public String getUsername() {
	    return fConnection.getUsername();
    }

	@Override
    public IRemoteConnectionWorkingCopy getWorkingCopy() {
	    return this;
    }

	@Override
    public boolean isOpen() {
	    return fConnection.isOpen();
    }

	@Override
    public void open(IProgressMonitor monitor) throws RemoteConnectionException {
	    fConnection.open(monitor);
    }

	@Override
    public void removeConnectionChangeListener(IRemoteConnectionChangeListener listener) {
		fConnection.removeConnectionChangeListener(listener);
    }
}
