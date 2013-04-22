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
 * Simulator service.
 * <p>
 * Allows to start/stop external simulators.
 * <p>
 * Simulator instance related UI parts, like configuration panels, are retrieved
 * by clients via the {@link IUIService}.
 */
public interface ISimulatorService extends IService {

	/**
	 * The constants for the simulator state.
	 */
	public enum State { Stopped, Starting, Started, Stopping }

	/**
	 * Starts the simulator.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param config The encoded simulator settings or <code>null</code>.
	 * @param callback The callback to invoke once the operation finishes. Must not be <code>null</code>.
	 * @param monitor The progress monitor or <code>null</code>.
	 */
	public void start(Object context, String config, ICallback callback, IProgressMonitor monitor);

	/**
	 * Stops the simulator.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param config The encoded simulator settings or <code>null</code>.
	 * @param callback The callback to invoke once the operation finishes. Must not be <code>null</code>.
	 * @param monitor The progress monitor or <code>null</code>.
	 */
	public void stop(Object context, String config, ICallback callback, IProgressMonitor monitor);

	/**
	 * Checks if the simulator is running.
	 * <p>
	 * The result of the check is return as {@link Boolean} object by the callback's {@link ICallback#getResult()} method.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param config The encoded simulator settings or <code>null</code>.
	 * @param callback The callback to invoke once the operation finishes. Must not be <code>null</code>.
	 */
	public void isRunning(Object context, String config, ICallback callback);

	/**
	 * Get the state of the simulator for the given context.
	 * @param context The context. Must not be <code>null</code>.
	 * @param config The encoded simulator settings or <code>null</code>.
	 * @return The simulator state.
	 */
	public State getState(Object context, String config);

	/**
	 * Get the default config for the simulator.
	 * The returned config does not need to be valid!
	 * @return The default config or <code>null</code>.
	 */
	public String getDefaultConfig();

	/**
	 * Validate a simulator config.
	 * @param context The context. Must not be <code>null</code>.
	 * @param config The config to validate.
	 * @return <code>true</code> if the config is valid.
	 */
	public boolean isValidConfig(Object context, String config);
}
