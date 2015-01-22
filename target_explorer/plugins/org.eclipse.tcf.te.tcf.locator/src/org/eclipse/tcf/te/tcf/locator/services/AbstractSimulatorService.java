/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.services;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;

/**
 * Abstract simulator service implementation.
 */
public abstract class AbstractSimulatorService extends AbstractService implements ISimulatorService {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService#useRunning(java.lang.Object, java.lang.String, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void useRunning(Object context, String config, ICallback callback, IProgressMonitor monitor) {
		callback.done(this, StatusHelper.getStatus(new UnsupportedOperationException("Using already running '" + getName() + "' is not supported."))); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
