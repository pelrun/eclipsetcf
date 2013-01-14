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
	 * Configure the simulator service instance.
	 * <p>
	 * The properties to set depends on the simulator service instance. Check
	 * the documentation of the simulator service instance implementation for
	 * details. If the given configuration is <code>null</code>, the simulator
	 * service instance should reset the configuration to it's defaults.
	 *
	 * @param config The simulator service configuration or <code>null</code>.
	 */
	public void setConfiguration(IPropertiesContainer config);

	/**
	 * Returns the simulator service instance configuration.
	 *
	 * @return The simulator service instance configuration.
	 */
	public IPropertiesContainer getConfiguration();

	/**
	 * Starts the simulator.
	 * <p>
	 * The simulation properties are retrieved from the given context object.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param callback The callback to invoke once the operation finishes. Must not be <code>null</code>.
	 * @param monitor The progress monitor or <code>null</code>.
	 */
	public void start(Object context, ICallback callback, IProgressMonitor monitor);

	/**
	 * Stops the simulator.
	 * <p>
	 * The simulation properties are retrieved from the given context object.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param callback The callback to invoke once the operation finishes. Must not be <code>null</code>.
	 * @param monitor The progress monitor or <code>null</code>.
	 */
	public void stop(Object context, ICallback callback, IProgressMonitor monitor);

	/**
	 * Checks if the simulator is running.
	 * <p>
	 * The simulation properties are retrieved from the given context object.
	 * <p>
	 * The result of the check is return as {@link Boolean} object by the callback's {@link ICallback#getResult()} method.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param callback The callback to invoke once the operation finishes. Must not be <code>null</code>.
	 */
	public void isRunning(Object context, ICallback callback);
}
