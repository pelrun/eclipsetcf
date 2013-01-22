/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.interfaces.services;

import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService;

/**
 * Simulator service UI delegate.
 */
public interface ISimulatorServiceUIDelegate {

	/**
	 * Get the simulator service the UI delegate is associated with.
	 *
	 * @return The simulator service.
	 */
	public ISimulatorService getService();

	/**
	 * Get the name of the simulator service to identify the simulator (type)
	 * to the user in the UI.
	 *
	 * @return The simulator service name.
	 */
	public String getName();

    /**
     * Configure the simulator.
     *
     * @param oldConfig The previous configuration or <code>null</code>.
     * @return The new configuration or <code>null</code>.
     */
    public String configure(String oldConfig);

    /**
     * Returns the default configuration of the simulator.
     *
     * @return The default configuration or <code>null</code>.
     */
    public String getDefaultConfiguration();
}
