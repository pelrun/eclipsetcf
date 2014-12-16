/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.remote.ui;

import org.eclipse.remote.core.IRemoteServices;
import org.eclipse.remote.ui.IRemoteUIConnectionManager;
import org.eclipse.remote.ui.IRemoteUIFileManager;
import org.eclipse.remote.ui.IRemoteUIServices;
import org.eclipse.tcf.te.tcf.remote.core.TCFRemoteServices;


public class TCFUIServices implements IRemoteUIServices {

	private TCFRemoteServices fServices;

	public TCFUIServices(IRemoteServices services) {
		fServices = (TCFRemoteServices) services;
	}

	@Override
	public String getId() {
		return fServices.getId();
	}

	@Override
	public String getName() {
		return fServices.getName();
	}

	@Override
	public IRemoteUIConnectionManager getUIConnectionManager() {
		return null;
	}

	@Override
	public IRemoteUIFileManager getUIFileManager() {
		return new TCFUIFileManager();
	}
}
