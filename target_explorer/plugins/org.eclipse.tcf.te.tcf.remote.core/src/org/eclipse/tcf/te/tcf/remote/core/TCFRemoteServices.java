/*******************************************************************************
 * Copyright (c) 2014, 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.remote.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.remote.core.AbstractRemoteServices;
import org.eclipse.remote.core.IRemoteServicesDescriptor;

public class TCFRemoteServices extends AbstractRemoteServices {
	public static final String TCF_ID = "org.eclipse.tcf.te.tcf.remote.core.TCFService"; //$NON-NLS-1$

	private final TCFConnectionManager fConnectionManager = new TCFConnectionManager(this);

	public TCFRemoteServices(IRemoteServicesDescriptor descriptor) {
		super(descriptor);
	}

	@Override
    public TCFConnectionManager getConnectionManager() {
		return fConnectionManager;
	}

	@Override
    public boolean initialize(IProgressMonitor monitor) {
		fConnectionManager.initialize();
		return true;
	}

	@Override
    public int getCapabilities() {
		return 0;
	}
}
