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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.remote.core.AbstractRemoteConnectionManager;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.IRemoteConnectionWorkingCopy;
import org.eclipse.remote.core.exception.RemoteConnectionException;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;

public class TCFConnectionManager extends AbstractRemoteConnectionManager implements IPeerModelListener, IEventListener {
	private final Map<String, TCFConnection> fConnections = Collections.synchronizedMap(new HashMap<String, TCFConnection>());
	private int fInitialized = 0;

	public TCFConnectionManager(TCFRemoteServices services) {
		super(services);
	}

	@Override
	protected TCFRemoteServices getRemoteServices() {
	    return (TCFRemoteServices) super.getRemoteServices();
	}

	public void initialize() {
		synchronized (fConnections) {
			if (fInitialized > 0)
				return;

			fInitialized = 1;

			Protocol.invokeLater(new Runnable() {
				@SuppressWarnings("synthetic-access")
                @Override
				public void run() {
					EventManager.getInstance().addEventListener(TCFConnectionManager.this, ChangeEvent.class);
					IPeerModel peerModel = ModelManager.getPeerModel();
					peerModel.addListener(TCFConnectionManager.this);

					synchronized (fConnections) {
						for (IPeerNode peerNode : peerModel.getPeerNodes()) {
							String name = peerNode.getPeer().getName();
							TCFConnection connection = fConnections.get(name);
							if (connection != null) {
								connection.setPeerNode(peerNode);
							} else {
								fConnections.put(name, new TCFConnection(getRemoteServices(), peerNode));
							}
						}
						for (Iterator<TCFConnection> it = fConnections.values().iterator(); it.hasNext(); ) {
							TCFConnection conn = it.next();
							if (conn.getPeerNode() == null)
								it.remove();
						}
						fInitialized = 2;
						fConnections.notifyAll();
					}
				}
			});
		}
    }

	public void waitForInitialization(IProgressMonitor monitor) {
		synchronized (fConnections) {
			if (fInitialized == 2)
				return;
			if (fInitialized == 0)
				initialize();

			while (fInitialized != 2) {
				if (monitor.isCanceled())
					return;
				try {
	                fConnections.wait(500);
                } catch (InterruptedException e) {
                	Thread.currentThread().interrupt();
                	return;
                }
			}
		}
	}

	@Override
    public void modelChanged(IPeerModel model, IPeerNode peerNode, boolean added) {
		String name = peerNode.getPeer().getName();
		if (added) {
			synchronized (fConnections) {
				if (!fConnections.containsKey(name)) {
					fConnections.put(name, new TCFConnection(getRemoteServices(), peerNode));
				}
			}
		} else {
			TCFConnection connection = fConnections.remove(name);
			if (connection != null) {
				connection.setConnectedTCF(false);
			}
		}
    }

	@Override
    public void modelDisposed(IPeerModel model) {
		fConnections.clear();
    }

	@Override
	public void eventFired(EventObject event) {
		final ChangeEvent changeEvent = (ChangeEvent) event;
        final Object source = changeEvent.getSource();
		if (source instanceof IPeerNode && IPeerNodeProperties.PROPERTY_CONNECT_STATE.equals(changeEvent.getEventId())) {
			IPeerNode peerNode = (IPeerNode) source;
			TCFConnection connection = fConnections.get(peerNode.getPeer().getName());
			if (connection != null) {
		    	 Object val= changeEvent.getNewValue();
		    	 boolean connected = val instanceof Number && ((Number) val).intValue() == IConnectable.STATE_CONNECTED;
		    	 connection.setConnectedTCF(connected);
			}
		}
	}

	@Override
	public IRemoteConnection getConnection(String name) {
		synchronized (fConnections) {
			TCFConnection connection = fConnections.get(name);
			if (connection == null && fInitialized < 2) {
				connection = new TCFConnection(getRemoteServices(), name);
				fConnections.put(name, connection);
			}
		}
		return fConnections.get(name);
	}

	@Override
	public IRemoteConnection getConnection(URI uri) {
		String connName = TCFEclipseFileSystem.getConnectionNameFor(uri);
		if (connName != null) {
			return getConnection(connName);
		}
		return null;
	}

	@Override
	public List<IRemoteConnection> getConnections() {
		synchronized (fConnections) {
			return new ArrayList<IRemoteConnection>(fConnections.values());
		}
	}

	@Override
	public IRemoteConnectionWorkingCopy newConnection(String name) throws RemoteConnectionException {
		throw new RemoteConnectionException(Messages.TCFConnectionManager_errorNoCreateConnection);
	}

	@Override
	public void removeConnection(IRemoteConnection conn) {
	}

	void open(IPeerNode peerNode, IProgressMonitor monitor) throws RemoteConnectionException {
		final boolean[] done = {false};
		Callback callback = new Callback() {
			@Override
			protected void internalDone(Object caller, IStatus status) {
				synchronized (done) {
					done[0] = true;
					done.notify();
				}
			}
		};
		synchronized (done) {
			peerNode.changeConnectState(IConnectable.ACTION_CONNECT, callback, monitor);
			try {
				if (!done[0])
					done.wait();
            } catch (InterruptedException e) {
            	throw new RemoteConnectionException(e);
            }
		}
		if (peerNode.getConnectState() != IConnectable.STATE_CONNECTED) {
			IStatus status = callback.getStatus();
			if (status != null && !status.isOK()) {
				String msg = status.getMessage();
				if (msg != null && msg.length() > 0) {
					throw new RemoteConnectionException(msg, status.getException());
				}
			}
			throw new RemoteConnectionException(Messages.TCFConnectionManager_errorCannotConnect, status != null ? status.getException() : null);
		}
    }
}
