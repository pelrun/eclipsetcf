/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.locator.nodes;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.core.utils.ConnectStateHelper;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService;
import org.eclipse.tcf.te.runtime.stepper.utils.StepperHelper;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModelProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IStepperServiceOperations;

/**
 * ConnectablePeerModel
 */
public class ConnectablePeerModel extends PeerModel implements IConnectable {

	public static final String PROPERTY_CONNECT_STATE = "connectState"; //$NON-NLS-1$

	/**
	 * Constructor.
	 * @param model
	 * @param peer
	 */
    public ConnectablePeerModel(ILocatorModel model, IPeer peer) {
	    super(model, peer);
	    setConnectState(STATE_DISCONNECTED);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.core.interfaces.IConnectable#getConnectState()
	 */
    @Override
    public int getConnectState() {
    	final AtomicInteger state = new AtomicInteger(STATE_UNKNOWN);
    	Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				if (getProperty(PROPERTY_CONNECT_STATE) != null) {
					state.set(getIntProperty(PROPERTY_CONNECT_STATE));
				}
			}
		});
    	return state.get();
    }

    @Override
    public boolean setConnectState(final int newState) {
    	final AtomicBoolean result = new AtomicBoolean(false);
    	if (isConnectStateChangeAllowed(newState)) {
	    	Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
			    	result.set(setProperty(PROPERTY_CONNECT_STATE, newState));
				}
			});
    	}
    	return result.get();
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.tcf.locator.nodes.PeerModel#postSetProperty(java.lang.String, java.lang.Object, java.lang.Object)
     */
    @Override
    public void postSetProperty(String key, Object value, Object oldValue) {
    	if (getConnectState() == STATE_CONNECTED && IPeerModelProperties.PROP_STATE.equals(key)) {
    		int state = value instanceof Integer ? ((Integer)value).intValue() : IPeerModelProperties.STATE_UNKNOWN;
    		if (state != IPeerModelProperties.STATE_CONNECTED && state != IPeerModelProperties.STATE_REACHABLE) {
    			changeConnectState(STATE_DISCONNECTED, null, null);
    		}
    	}

        super.postSetProperty(key, value, oldValue);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.core.interfaces.IConnectable#changeConnectState(int, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
    @Override
    public void changeConnectState(final int action, ICallback callback, IProgressMonitor monitor) throws IllegalArgumentException {
		final int oldState = getConnectState();
    	if (!isConnectStateChangeActionAllowed(action)) {
    		IllegalArgumentException e = new IllegalArgumentException("Cannot change state from '" + ConnectStateHelper.getConnectState(oldState) + "' using action '" + ConnectStateHelper.getConnectState(action) + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    		if (callback != null) {
    			callback.done(this, StatusHelper.getStatus(e));
    		}
    		else {
    			throw e;
    		}
    	}

    	String operation = null;
    	int intermediateState = 0;

    	switch (action) {
		case ACTION_CONNECT:
			operation = IStepperServiceOperations.CONNECT;
			intermediateState = STATE_CONNECT_SCHEDULED;
			break;
		case ACTION_DISCONNECT:
			operation = IStepperServiceOperations.DISCONNECT;
			intermediateState = STATE_DISCONNECT_SCHEDULED;
			break;
		}

    	IStepperOperationService service = StepperHelper.getService(this, operation);
    	if (service != null) {
    		setConnectState(intermediateState);
    		StepperHelper.scheduleStepperJob(this, operation, service, callback, monitor);
    	}
    	else if (callback != null) {
    		callback.done(this, StatusHelper.getStatus(new NullPointerException("Missing stepper operation service for " + getName() + "."))); //$NON-NLS-1$ //$NON-NLS-2$
    	}
    }

    @Override
    public boolean isConnectStateChangeActionAllowed(int action) {
    	int state = getConnectState();
    	switch (state) {
			case STATE_CONNECTED:
			case STATE_CONNECT_SCHEDULED:
			case STATE_CONNECTING:
				return isAllowedState(action, ACTION_DISCONNECT);
			case STATE_DISCONNECTED:
				return isAllowedState(action, ACTION_CONNECT);
    	}
    	return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.core.interfaces.IConnectable#isConnectStateChangeAllowed(int)
     */
    @Override
    public boolean isConnectStateChangeAllowed(int newState) {
    	int state = getConnectState();
    	switch (state) {
			case STATE_CONNECTED:
				return isAllowedState(newState, STATE_DISCONNECTED, STATE_DISCONNECT_SCHEDULED, STATE_DISCONNECTING);
			case STATE_CONNECT_SCHEDULED:
				return isAllowedState(newState, STATE_CONNECTING, STATE_CONNECTED, STATE_DISCONNECTED, STATE_DISCONNECT_SCHEDULED, STATE_DISCONNECTING);
			case STATE_CONNECTING:
				return isAllowedState(newState, STATE_CONNECTED, STATE_DISCONNECT_SCHEDULED, STATE_DISCONNECTING, STATE_DISCONNECTED);
			case STATE_DISCONNECTED:
				return isAllowedState(newState, STATE_CONNECTED, STATE_CONNECT_SCHEDULED, STATE_CONNECTING);
			case STATE_DISCONNECT_SCHEDULED:
				return isAllowedState(newState, STATE_DISCONNECTING, STATE_DISCONNECTED);
			case STATE_DISCONNECTING:
				return isAllowedState(newState, STATE_DISCONNECTED);
			case STATE_UNKNOWN:
				return isAllowedState(newState, STATE_DISCONNECTED);
		}
        return false;
    }

    private boolean isAllowedState(int state, int... allowedStates) {
    	for (int allowedState : allowedStates) {
	        if (state == allowedState) {
	        	return true;
	        }
        }
    	return false;
    }
}
