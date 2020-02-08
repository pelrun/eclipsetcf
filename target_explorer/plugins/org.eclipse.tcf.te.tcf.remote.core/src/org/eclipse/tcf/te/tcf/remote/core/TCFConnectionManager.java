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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.IRemoteConnectionType;
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
import org.eclipse.tcf.te.tcf.remote.core.activator.CoreBundleActivator;

public class TCFConnectionManager implements IPeerModelListener, IEventListener {

	public static final TCFConnectionManager INSTANCE = new TCFConnectionManager();

	private final Map<String, TCFConnection> fConnections = Collections.synchronizedMap(new HashMap<String, TCFConnection>());
	private IRemoteConnectionType fConnectionType;
	private int fInitialized = 0;

	public TCFConnection mapConnection(IRemoteConnection rc) {
		if (rc == null)
			return null;
		if (!rc.getConnectionType().getId().equals(TCFConnection.CONNECTION_TYPE_ID))
			return null;

		synchronized(fConnections) {
			String name = rc.getName();
			TCFConnection result = fConnections.get(name);
			if (result == null) {
				result = new TCFConnection(rc);
				fConnections.put(name, result);
			}
			return result;
		}
	}

	public void setConnectionType(IRemoteConnectionType connectionType) {
		synchronized(fConnections) {
			fConnectionType = connectionType;
			initialize();
			syncConnections();
		}
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
							if (connection == null) {
								fConnections.put(name, new TCFConnection(peerNode));
							} else {
								connection.setPeerNode(peerNode);
							}
						}
						fInitialized = 2;
						syncConnections();
						fConnections.notifyAll();
					}
				}

			});
		}
    }

	void syncConnections() {
		if (fConnectionType != null && fInitialized == 2) {
			// Remove all connections without a peer
			for (IRemoteConnection rc : new ArrayList<IRemoteConnection>(fConnectionType.getConnections())) {
				String name = rc.getName();
				TCFConnection connection = fConnections.get(name);
				if (connection == null || connection.getPeerNode() == null) {
					try {
						fConnectionType.removeConnection(rc);
					} catch (RemoteConnectionException e) {
						CoreBundleActivator.logError("Cannot remove remote connection.", e); //$NON-NLS-1$
					}
					fConnections.remove(name);
				}
			}
			// Add connections with peers
			for (Iterator<TCFConnection> it = fConnections.values().iterator(); it.hasNext(); ) {
				TCFConnection connection = it.next();
				IPeerNode peerNode = connection.getPeerNode();
				if (peerNode == null) {
					it.remove();
				} else {
					addRemoteConnection(connection);
				}
			}
		}
	}

	private void addRemoteConnection(TCFConnection connection) {
		if (fConnectionType == null)
			return;

		String name = connection.getName();
		if (fConnectionType.getConnection(name) == null) {
			try {
				IRemoteConnectionWorkingCopy wc = fConnectionType.newConnection(name);
				IRemoteConnection rc = wc.save();
				connection.setRemoteConnection(rc);
			} catch (RemoteConnectionException e) {
				CoreBundleActivator.logError("Cannot add remote connection.", e); //$NON-NLS-1$
			}
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
				TCFConnection connection = fConnections.get(name);
				if (connection != null) {
					connection.setPeerNode(peerNode);
				} else {
					connection = new TCFConnection(peerNode);
					fConnections.put(name, connection);
				}
				addRemoteConnection(connection);
			}
		} else {
			TCFConnection connection = fConnections.remove(name);
			if (connection != null) {
				connection.setConnectedTCF(false);
				IRemoteConnection rc = connection.getRemoteConnection();
				if (rc != null) {
					try {
						rc.getConnectionType().removeConnection(rc);
					} catch (RemoteConnectionException e) {
						CoreBundleActivator.logError("Cannot remove remote connection.", e); //$NON-NLS-1$
					}
				}
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

	void close(IPeerNode peerNode) {
		peerNode.changeConnectState(IConnectable.ACTION_DISCONNECT, new Callback(), null);
    }

}
