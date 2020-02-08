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

import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;

/**
 * The service to refresh the parent locator model from remote.
 */
public interface ILocatorModelRefreshService extends ILocatorModelService {

	/**
	 * Refreshes the list of known peers from the local locator service
	 * and update the locator model.
	 *
	 * @param callback The callback to invoke once the refresh operation finished, or <code>null</code>.
	 */
	public void refresh(ICallback callback);

	/**
	 * Refreshes the given locator node and its list of known children from the local locator service.
	 *
	 * @param locatorNode The locator node to refresh.
	 * @param callback The callback to invoke once the refresh operation finished, or <code>null</code>.
	 */
	public void refresh(ILocatorNode locatorNode, ICallback callback);
}
