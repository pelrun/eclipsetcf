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

package org.eclipse.tcf.te.tcf.remote.core.operation;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.tcf.processes.core.launcher.ProcessLauncher;


public class TCFOperationStartProcess extends TCFOperation<Object> {

	private final ProcessLauncher fLauncher;
	private final IPeer fPeer;
	private final IPropertiesContainer fProps;

	public TCFOperationStartProcess(IPeer peer, ProcessLauncher launcher, IPropertiesContainer launcherProps) {
		fPeer = peer;
	    fLauncher = launcher;
	    fProps = launcherProps;
    }

	@Override
	protected void doExecute() {
		fLauncher.launch(fPeer, fProps, new Callback() {
			@Override
			protected void internalDone(Object caller, IStatus status) {
				if (shallAbort(status))
					return;
			    TCFOperationStartProcess.this.setResult(null);
			}
		});
	}
}
