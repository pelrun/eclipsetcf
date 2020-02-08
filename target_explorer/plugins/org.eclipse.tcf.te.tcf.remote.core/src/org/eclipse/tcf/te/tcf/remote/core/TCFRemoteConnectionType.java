/*******************************************************************************
 * Copyright (c) 2014, 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.remote.core;

import org.eclipse.remote.core.IRemoteConnectionProviderService;
import org.eclipse.remote.core.IRemoteConnectionType;

public class TCFRemoteConnectionType implements IRemoteConnectionProviderService {
	private final IRemoteConnectionType fConnectionType;

	public TCFRemoteConnectionType(IRemoteConnectionType type) {
		fConnectionType = type;
	}

	@Override
	public IRemoteConnectionType getConnectionType() {
		return fConnectionType;
	}

	@Override
	public void init() {
		TCFConnectionManager.INSTANCE.setConnectionType(fConnectionType);
	}

}
