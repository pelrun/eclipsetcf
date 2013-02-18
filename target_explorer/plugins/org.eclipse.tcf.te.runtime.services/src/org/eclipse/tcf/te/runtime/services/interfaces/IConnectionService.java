/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.services.interfaces;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;

/**
 * Connection service.
 * <p>
 * Allows to connect or disconnect to a given connectable context.
 */
public interface IConnectionService extends IService {

	/**
	 * The constants for the (cached) connection state.
	 */
	public enum State { Disconnected, Connecting, Connected, Disconnecting }

	/**
	 * Returns the connection state of the given connectable context.
	 *
	 * @param context The connectable context. Must not be <code>null</code>.
	 * @return The connection state.
	 */
	public State getState(Object context);

	/**
	 * Connects the given connectable context.
	 * <p>
	 * If the given context is in connecting state, the callback is invoked once the
	 * connectable enters the connected state.
	 * <p>
	 * If the given context is in connected state, the callback is invoked immediately.
	 * <p>
	 * If the given context is in disconnecting state, the callback is invoked immediately
	 * with an cancel status.
	 *
	 * @param context The connectable context. Must not be <code>null</code>.
	 * @param callback The callback. Must not be <code>null</code>.
	 * @param monitor The progress monitor or <code>null</code>.
	 */
	public void connect(Object context, ICallback callback, IProgressMonitor monitor);

	/**
	 * Disconnects the given connectable context.
	 * <p>
	 * If the given context is in disconnecting state, the callback is invoked once the
	 * connectable enters the disconnected state.
	 * <p>
	 * If the given context is in disconnected state, the callback is invoked immediately.
	 * <p>
	 * If the given context is in connecting state, the connect sequence is aborted and
	 * rolled back. The callback is invoked once the connectable enters the disconnected state.
	 *
	 * @param context The connectable context. Must not be <code>null</code>.
	 * @param callback The callback. Must not be <code>null</code>.
	 * @param monitor The progress monitor or <code>null</code>.
	 */
	public void disconnect(Object context, ICallback callback, IProgressMonitor monitor);
}
