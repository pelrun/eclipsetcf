/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.processes.core.interfaces;

import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;

/**
 * IProcessContextItem
 */
public interface IProcessContextItem extends IPropertiesContainer {

	public static final String PROPERTY_ID = "id"; //$NON-NLS-1$
	public static final String PROPERTY_PID = "pid"; //$NON-NLS-1$
	public static final String PROPERTY_NAME = "name"; //$NON-NLS-1$
	public static final String PROPERTY_PATH = "path"; //$NON-NLS-1$

	public static final String PATH_SEPARATOR = "/"; //$NON-NLS-1$

	/**
	 * Return the context id.
	 */
	public String getId();

	/**
	 * Return the context name.
	 */
	public String getName();

	/**
	 * Return the context path.
	 */
	public String getPath();
}
