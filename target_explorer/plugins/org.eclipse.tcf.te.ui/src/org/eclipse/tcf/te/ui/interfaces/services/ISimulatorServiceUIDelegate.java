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

import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService;

/**
 * Simulator service UI delegate.
 */
public interface ISimulatorServiceUIDelegate {

	public static final String PROP_MODES = "Modes"; //$NON-NLS-1$
	public static final String PROP_MODE_LABEL_X = "ModeLabel"; //$NON-NLS-1$
	public static final String PROP_MODE_DESCRIPTION_X = "ModeDescription"; //$NON-NLS-1$

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
	 * Get a description fo rthe given config.
	 * This description is shown i.e. as tooltip of the configure button.
     * @param context The context for which the simulator should be configured.
     * @param config The configuration or <code>null</code>.
	 * @return The description of the given config.
	 */
	public String getDescription(Object context, String config);

	/**
	 * Get properties for ui configuration.
	 * @param context The conetxt.
	 * @param config The current config.
	 * @return The properties for ui configuartion
	 */
	public IPropertiesContainer getProperties(Object context, String config);
}
