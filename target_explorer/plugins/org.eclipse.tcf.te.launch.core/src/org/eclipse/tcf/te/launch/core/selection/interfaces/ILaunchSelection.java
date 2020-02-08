/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.core.selection.interfaces;


/**
 * A launch selection.
 */
public interface ILaunchSelection {

	/**
	 * Returns the launch mode for which the selection has been created.
	 *
	 * @return The launch mode or <code>null</code>
	 */
	public String getLaunchMode();

	/**
	 * Returns all contexts for this selection.
	 *
	 * @return An array with all selection contexts or an empty array.
	 */
	public ISelectionContext[] getSelectedContexts();

	/**
	 * Returns all contexts of a specified type for this selection.
	 *
	 * @return An array with all selection contexts of the specified type, or an empty array.
	 */
	public ISelectionContext[] getSelectedContexts(Class<?> type);
}
