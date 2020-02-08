/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.services.interfaces;

/**
 * Menu services.
 * <p>
 * Allows to control specific menu contributions for a selection.
 */
public interface IMenuService extends IService {

	/**
	 * Tests if or if not the menu contribution identified by the given id is
	 * visible for the given context.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param contributionID The contribution ID. Must not be <code>null</code>.
	 *
	 * @return <code>True</code> if the contribution is visible, <code>false</code> otherwise.
	 */
	public boolean isVisible(Object context, String contributionID);
}
