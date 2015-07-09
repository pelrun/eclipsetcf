/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.remote.ui;

import org.eclipse.remote.core.IRemoteConnectionType;
import org.eclipse.remote.ui.AbstractRemoteUIConnectionService;
import org.eclipse.remote.ui.IRemoteUIConnectionWizard;
import org.eclipse.swt.widgets.Shell;

public class TCFUIConnectionService extends AbstractRemoteUIConnectionService {

	private IRemoteConnectionType fConnectionType;

	public TCFUIConnectionService(IRemoteConnectionType connectionType) {
		fConnectionType = connectionType;
	}

	@Override
	public IRemoteUIConnectionWizard getConnectionWizard(Shell shell) {
		return null;
	}

	@Override
	public IRemoteConnectionType getConnectionType() {
		return fConnectionType;
	}
}
