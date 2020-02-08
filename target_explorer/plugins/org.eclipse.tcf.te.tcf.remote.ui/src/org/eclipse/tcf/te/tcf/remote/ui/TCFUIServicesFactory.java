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
package org.eclipse.tcf.te.tcf.remote.ui;

import org.eclipse.remote.core.IRemoteConnectionType;
import org.eclipse.remote.core.IRemoteConnectionType.Service;

public class TCFUIServicesFactory implements IRemoteConnectionType.Service.Factory {

	@Override
	public <T extends Service> T getService(IRemoteConnectionType connectionType, Class<T> service) {
		if (service.isAssignableFrom(TCFUIFileService.class))
			return service.cast(new TCFUIFileService(connectionType));
		if (service.isAssignableFrom(TCFUIConnectionService.class))
			return service.cast(new TCFUIConnectionService(connectionType));
		return null;
	}
}
