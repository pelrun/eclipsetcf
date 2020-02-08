/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.ui.interfaces.data;

import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;

/**
 * Public interface for wizard or dialog pages, panel, controls or other UI elements
 * exchanging data via a shared data object.
 */
public interface IUpdatable {

	/**
	 * Update the data from the given properties container which contains the current
	 * working data.
	 * <p>
	 * This method may called multiple times during the lifetime of the node and the given
	 * properties container might be even <code>null</code>.
	 *
	 * @param data
	 */
	public void updateData(IPropertiesContainer data);

}
