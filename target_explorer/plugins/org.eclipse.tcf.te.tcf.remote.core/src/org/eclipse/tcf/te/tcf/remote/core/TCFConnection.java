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

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.IRemoteConnectionChangeEvent;
import org.eclipse.remote.core.IRemoteConnectionChangeListener;
import org.eclipse.remote.core.IRemoteConnectionWorkingCopy;
import org.eclipse.remote.core.IRemoteFileManager;
import org.eclipse.remote.core.IRemoteProcess;
import org.eclipse.remote.core.IRemoteProcessBuilder;
import org.eclipse.remote.core.exception.RemoteConnectionException;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.remote.core.operation.TCFOperationGetEnvironment;

public class TCFConnection extends TCFConnectionBase {

	private static enum EState {OPEN, CLOSED_TCF, CLOSED_REMOTE_SERVICES}

	private final String fName;
    private volatile IPeerNode fPeerNode;
	private final List<IRemoteConnectionChangeListener> fListeners = new ArrayList<IRemoteConnectionChangeListener>();

	private volatile EState fState;
	private Map<String, String> fAttributes;
	private Map<String, String> fEnvironment;

	public TCFConnection(TCFRemoteServices tcfServices, IPeerNode peerNode) {
    	super(tcfServices);
    	fName = peerNode.getName();
    	fPeerNode = peerNode;
    	fState = peerNode.getConnectState() == IConnectable.STATE_CONNECTED ? EState.OPEN : EState.CLOSED_TCF;
    }

    public TCFConnection(TCFRemoteServices tcfServices, String name) {
    	super(tcfServices);
    	fName = name;
    	fState = EState.CLOSED_TCF;
    }

	public void setPeerNode(IPeerNode peerNode) {
		fPeerNode = peerNode;
		setConnectedTCF(peerNode.getConnectState() == IConnectable.STATE_CONNECTED);
	}

	public IPeerNode getPeerNode() {
	    return fPeerNode;
	}

	@Override
	public String getAddress() {
		if (fPeerNode == null)
			return "0.0.0.0"; //$NON-NLS-1$

		return fPeerNode.getPeer().getAttributes().get(IPeer.ATTR_IP_HOST);
	}

	@Override
	public Map<String, String> getAttributes() {
		if (fPeerNode == null)
			return emptyMap();

		if (fAttributes == null) {
			Map<String, String> attributes = new HashMap<String, String>();
			attributes.put(OS_NAME_PROPERTY, fPeerNode.getPeer().getOSName());
			fAttributes = unmodifiableMap(attributes);
		}
		return fAttributes;
	}

	@Override
	public IRemoteProcess getCommandShell(int flags) throws IOException {
		throw new IOException(Messages.TCFConnection_errorNoCommandShell);
	}

	@Override
	public Map<String, String> getEnv() {
		if (fEnvironment == null && fPeerNode != null) {
			try {
	            fEnvironment = new TCFOperationGetEnvironment(fPeerNode.getPeer()).execute(SubMonitor.convert(null));
            } catch (OperationCanceledException e) {
            } catch (CoreException e) {
            	Activator.logError(Messages.TCFConnection_errorNoEnvironment, e);
            }
		}
		return fEnvironment;
	}

	@Override
	public IRemoteFileManager getFileManager() {
		return new TCFFileManager(this);
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public int getPort() {
		if (fPeerNode == null)
			return 0;

		try {
			return Integer.parseInt(fPeerNode.getPeer().getAttributes().get(IPeer.ATTR_IP_PORT));
		} catch (Exception e) {
			return 0;
		}
	}

	@Override
	public IRemoteProcessBuilder getProcessBuilder(List<String> command) {
		return new TCFProcessBuilder(this, command);
	}

	@Override
	public IRemoteProcessBuilder getProcessBuilder(String... command) {
		return new TCFProcessBuilder(this, command);
	}

	@Override
	public String getUsername() {
		if (fPeerNode == null)
			return null;
		return fPeerNode.getPeer().getAttributes().get(IPeer.ATTR_USER_NAME);
	}

	@Override
	public IRemoteConnectionWorkingCopy getWorkingCopy() {
		return new TCFConnectionWorkingCopy(this);
	}

	@Override
	public boolean isOpen() {
		return fState == EState.OPEN;
	}

	@Override
	public void open(IProgressMonitor monitor) throws RemoteConnectionException {
		if (fPeerNode == null) {
			getRemoteServices().getConnectionManager().waitForInitialization(monitor);
		}
		boolean notify = false;
		boolean performOpen = false;
		synchronized (this) {
			if (fState != EState.OPEN) {
				if (fPeerNode.getConnectState() == IConnectable.STATE_CONNECTED) {
					fState = EState.OPEN;
					notify = true;
				} else {
					fState = EState.CLOSED_TCF;
					performOpen = true;
				}
			}
		}
		if (notify) {
			fireConnectionChangeEvent(IRemoteConnectionChangeEvent.CONNECTION_OPENED);
		} else if (performOpen) {
			getRemoteServices().getConnectionManager().open(fPeerNode, monitor);
		}
	}

	@Override
	public void close() {
		boolean notify = false;
		synchronized (this) {
			if (fState == EState.OPEN) {
				fState = EState.CLOSED_REMOTE_SERVICES;
				notify = true;
			}
		}
		if (notify) {
			fireConnectionChangeEvent(IRemoteConnectionChangeEvent.CONNECTION_CLOSED);
		}
	}

	void setConnectedTCF(boolean connected) {
		int notify = -1;
		synchronized (this) {
			if (connected) {
				if (fState == EState.CLOSED_TCF) {
					fState = EState.OPEN;
					notify = IRemoteConnectionChangeEvent.CONNECTION_OPENED;
				}
			} else {
				if (fState == EState.OPEN) {
					fState = EState.CLOSED_TCF;
					notify = IRemoteConnectionChangeEvent.CONNECTION_CLOSED;
				}
			}
		}
		if (notify != -1) {
			fireConnectionChangeEvent(notify);
		}
    }


	@Override
	public void addConnectionChangeListener(IRemoteConnectionChangeListener listener) {
		synchronized (fListeners) {
			if (!fListeners.contains(listener))
				fListeners.add(listener);
		}
	}

	@Override
	public void removeConnectionChangeListener(IRemoteConnectionChangeListener listener) {
		synchronized (fListeners) {
			fListeners.remove(listener);
		}
	}

	private IRemoteConnectionChangeListener[] getListeners() {
		synchronized (fListeners) {
			return fListeners.toArray(new IRemoteConnectionChangeListener[fListeners.size()]);
		}
	}

	@Override
	public void fireConnectionChangeEvent(final int type) {
		final IRemoteConnection connection = this;
		new Job(Messages.TCFConnection_notifyListeners) {
			@SuppressWarnings("synthetic-access")
            @Override
			protected IStatus run(IProgressMonitor monitor) {
				IRemoteConnectionChangeEvent event = new IRemoteConnectionChangeEvent() {
					@Override
					public IRemoteConnection getConnection() {
						return connection;
					}

					@Override
					public int getType() {
						return type;
					}
				};
				for (Object listener : getListeners()) {
					((IRemoteConnectionChangeListener) listener).connectionChanged(event);
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}
}
