/*******************************************************************************
 * Copyright (c) 2013, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.services.interfaces;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;

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
	 * Property: The associated simulator instance.
	 */
	public static final String PROP_SIM_INSTANCE = ISimulatorService.class.getName() + ".simInstance"; //$NON-NLS-1$

	/**
	 * The constants for the simulator state.
	 */
	public enum State { Stopped, Starting, Started, Stopping }

	/**
	 * Get the name of the simulator.
	 */
	public String getName();

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
	 * Use a running simulator.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param config The encoded simulator settings or <code>null</code>.
	 * @param callback The callback to invoke once the operation finishes. Must not be <code>null</code>.
	 * @param monitor The progress monitor or <code>null</code>.
	 */
	public void useRunning(Object context, String config, ICallback callback, IProgressMonitor monitor);

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
	 * Cleanup after stop or simulator killed.
	 * Should be called from stop.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param config The encoded simulator settings or <code>null</code>.
	 */
	public void cleanup(Object context, String config);

	/**
	 * Checks if the simulator is running.
	 * <p>
	 * The result of the check is return as {@link Boolean} object by the callback's {@link ICallback#getResult()} method.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param config The encoded simulator settings or <code>null</code>.
	 * @param callback The callback to invoke once the operation finishes. Must not be <code>null</code>.
	 * @param monitor The progress monitor or <code>null</code>.
	 */
	public void isRunning(Object context, String config, ICallback callback, IProgressMonitor monitor);

	/**
	 * Get the state of the simulator for the given context.
	 * @param context The context. Must not be <code>null</code>.
	 * @param config The encoded simulator settings or <code>null</code>.
	 * @return The simulator state.
	 */
	public State getState(Object context, String config);

	/**
	 * Get the default configuration for the simulator.
	 * <p>
	 * The returned configuration does not need to be valid!
	 *
	 * @return The default configuration or <code>null</code>.
	 */
	public String getDefaultConfig();

	/**
	 * Get the address data for the given simulator config.
	 * @param context The context. Must not be <code>null</code>.
	 * @param config The encoded simulator settings or <code>null</code>.
	 * @param currentAddress The current address data.
	 * @return The new simulator address data.
	 */
	public IPropertiesContainer getSimulatorAddress(Object context, String config, IPropertiesContainer currentAddress);

	/**
	 * Validate a simulator configuration
	 * .
	 * @param context The context. Must not be <code>null</code>.
	 * @param config The configuration to validate.
	 * @param checkContextAttributes <code>true</code> if attributes stored in the context should be checked too.
	 *
	 * @return <code>true</code> if the configuration is valid.
	 */
	public boolean isValidConfig(Object context, String config, boolean checkContextAttributes);
}
