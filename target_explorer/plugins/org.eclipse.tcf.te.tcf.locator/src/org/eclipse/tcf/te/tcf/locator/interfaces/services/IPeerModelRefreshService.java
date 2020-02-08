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

/**
 * The service to refresh the parent peer model.
 */
public interface IPeerModelRefreshService extends IPeerModelService {

	/**
	 * Refreshes the list of known peer nodes and update the peer model.
	 *
	 * @param callback The callback to invoke once the refresh operation finished, or <code>null</code>.
	 */
	public void refresh(ICallback callback);
}
