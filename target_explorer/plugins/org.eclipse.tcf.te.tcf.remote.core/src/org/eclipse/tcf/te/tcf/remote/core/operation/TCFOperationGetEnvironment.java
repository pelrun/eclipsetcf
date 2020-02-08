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

import static java.text.MessageFormat.format;

import java.util.Map;

import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.services.IProcesses.DoneGetEnvironment;
import org.eclipse.tcf.services.IProcessesV1;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.remote.core.nls.Messages;


public class TCFOperationGetEnvironment extends TCFOperation<Map<String,String>> {

	private final IPeer fPeer;

	public TCFOperationGetEnvironment(IPeer peer) {
		fPeer = peer;
    }

	@Override
	protected void doExecute() {
		IChannel channel = Tcf.getChannelManager().getChannel(fPeer);
		if (channel == null) {
			setError(createStatus(format(Messages.TCFOperationGetEnvironment_errorNoChannel, fPeer.getName()), null));
			return;
		}

		IProcesses psvc = channel.getRemoteService(IProcessesV1.class);
		if (psvc == null) {
			psvc = channel.getRemoteService(IProcesses.class);
		}
		if (psvc == null) {
			setError(createStatus(format(Messages.TCFOperationGetEnvironment_errorNoProcessesService, fPeer.getName()), null));
			return;
		}

		psvc.getEnvironment(new DoneGetEnvironment() {
			@Override
			public void doneGetEnvironment(IToken token, Exception error, Map<String, String> environment) {
				if (shallAbort(error))
					return;
				setResult(environment);
			}
		});
	}
}
