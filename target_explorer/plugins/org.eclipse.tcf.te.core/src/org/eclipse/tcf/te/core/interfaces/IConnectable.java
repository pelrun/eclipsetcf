/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.core.interfaces;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;

/**
 * IConnectable
 */
public interface IConnectable {

	// intermediate states are always < 0
	public static final int STATE_DISCONNECT_SCHEDULED = -1;
	public static final int STATE_DISCONNECTING = -2;

	public static final int STATE_CONNECT_SCHEDULED = -11;
	public static final int STATE_CONNECTING = -12;

	public static final int STATE_CONNECTION_RECOVERING = -21;

	// main states
	public static final int STATE_UNKNOWN = 0;
	public static final int STATE_DISCONNECTED = 1;
	public static final int STATE_CONNECTED = 11;
	public static final int STATE_CONNECTION_LOST = 21;

	// state change actions
	public static final int ACTION_UNKNOWN = STATE_UNKNOWN;

	public static final int ACTION_DISCONNECT = STATE_DISCONNECTED;
	public static final int ACTION_CONNECT = STATE_CONNECTED;

	/**
	 * Get the current connect state.
	 *
	 * @return The current connect state.
	 */
	public int getConnectState();

	/**
	 * Set the connect state property of the IConnectable.
	 * @param state The state.
	 * @return <code>true</code> if the state was set.
	 */
	public boolean setConnectState(int state);

	/**
	 * Change the current connect state to the new state if the given action is possible.
	 * Only actions >= 0 are allowed.
	 *
	 * @param action The state change action.
	 * @param callback The callback to indicate that the state change has been finished.
	 * @param monitor The progress monitor. If <code>null</code> a new job will be scheduled.
	 *
	 * @throws IllegalArgumentException if the given action is not allowed (only actions >= 0 are allowed)
	 */
	public void changeConnectState(int action, ICallback callback, IProgressMonitor monitor) throws IllegalArgumentException;

	/**
	 * Check whether a state change to the given state is allowed.
	 *
	 * @param state The new state to check.
	 * @return <code>true</code> if a state change to the given state is currently allowed.
	 */
	public boolean isConnectStateChangeAllowed(int state);

	/**
	 * Check whether a state change action is allowed.
	 *
	 * @param action The action to execute.
	 * @return <code>true</code> if a state change action is currently allowed.
	 */
    public boolean isConnectStateChangeActionAllowed(int action);

}
