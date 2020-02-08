/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces.services;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;

/**
 * Common parent interface for locator model services.
 */
public interface ILocatorModelService extends IAdaptable {

	/**
	 * Returns the parent locator model.
	 *
	 * @return The parent locator model.
	 */
	public ILocatorModel getLocatorModel();
}
