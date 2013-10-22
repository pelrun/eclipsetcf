/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime;

import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelRefreshService;

/**
 * Interface to be implemented by processes runtime model refresh services.
 */
public interface IRuntimeModelRefreshService extends IModelRefreshService {

	/**
	 * Auto refresh the content of the model from the top. It search for
	 * all nodes with query state "done" and refresh them one by one.
	 *
	 * @param callback The callback to invoke once the refresh operation finished, or <code>null</code>.
	 */
	public void autoRefresh(ICallback callback);
}
