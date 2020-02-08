/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.services.interfaces;

/**
 * Common service.
 */
public interface IService {

	/**
	 * Sets the service contribution id.
	 * <p>
	 * <b>Note:</b> Once set to a non-null value, the service id cannot be changed anymore.
	 *
	 * @param id The id or <code>null</code>.
	 */
	public void setId(String id);

	/**
	 * Returns the service contribution id.
	 *
	 * @return The id or <code>null</code> if the service id is not yet set.
	 */
	public String getId();
}
