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
package org.eclipse.tcf.te.tcf.remote.ui;

import org.eclipse.remote.core.IRemoteServices;
import org.eclipse.remote.ui.IRemoteUIServices;
import org.eclipse.remote.ui.IRemoteUIServicesFactory;

public class TCFUIServicesFactory implements IRemoteUIServicesFactory {
	@Override
    public IRemoteUIServices getServices(IRemoteServices services) {
		return new TCFUIServices(services);
	}
}
