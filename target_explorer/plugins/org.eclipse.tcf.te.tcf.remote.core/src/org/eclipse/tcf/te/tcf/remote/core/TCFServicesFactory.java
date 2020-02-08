/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.remote.core;

import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.IRemoteConnectionType;
import org.eclipse.remote.core.IRemoteProcess;

public class TCFServicesFactory implements IRemoteConnectionType.Service.Factory,
		IRemoteConnection.Service.Factory, IRemoteProcess.Service.Factory {

	@Override
	public <T extends IRemoteConnectionType.Service> T getService(IRemoteConnectionType connectionType, Class<T> service) {
		if (service.isAssignableFrom(TCFRemoteConnectionType.class)) {
			return service.cast(new TCFRemoteConnectionType(connectionType));
		}
		return null;
	}

	@Override
	public <T extends IRemoteConnection.Service> T getService(IRemoteConnection remoteConnection, Class<T> service) {
		if (service.isAssignableFrom(TCFConnection.class)) {
			return service.cast(TCFConnectionManager.INSTANCE.mapConnection(remoteConnection));
		}
		return null;
	}

	@Override
	public <T extends IRemoteProcess.Service> T getService(IRemoteProcess remoteProcess, Class<T> service) {
		return remoteProcess.getService(service);
	}
}
