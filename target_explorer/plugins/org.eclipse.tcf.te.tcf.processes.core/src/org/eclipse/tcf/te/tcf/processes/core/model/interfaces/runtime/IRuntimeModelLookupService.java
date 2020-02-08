/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime;

import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelLookupService;

/**
 * Common interface to be implemented by a model lookup service.
 */
public interface IRuntimeModelLookupService extends IModelLookupService {

	public static final String CAPABILITY_THREAD_CREATION = "ThreadCreation"; //$NON-NLS-1$
	public static final String CAPABILITY_PROCESS_CREATION = "ProcessCreation"; //$NON-NLS-1$

	/**
	 * Search the associated model for the process context root node
	 * providing the given capabilities.
	 * <p>
	 * The callback result contains either the process context root node or <code>null</code>.
	 *
	 * @param capabilities The capabilities. Must not be <code>null</code> and must not be empty.
	 * @param callback The callback. Must not be <code>null</code>.
	 */
    public void lkupModelNodeByCapability(String[] capabilities, ICallback callback);

}
