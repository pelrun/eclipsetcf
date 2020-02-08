/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.handler;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.runtime.services.interfaces.IDebugService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.steps.StartDebuggerStep.IDelegate;

/**
 * Start debugger command handler implementation.
 */
public class StartDebugCommandHandler extends AbstractPeerNodeCommandHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.handler.AbstractPeerNodeCommandHandler#internalExecute(org.eclipse.core.commands.ExecutionEvent, org.eclipse.jface.viewers.IStructuredSelection, java.util.List)
	 */
	@Override
	protected Object internalExecute(ExecutionEvent event, IStructuredSelection selection, List<IPeerNode> peerNodes) {
		for (final IPeerNode peerNode : peerNodes) {
			IDebugService dbgService = ServiceManager.getInstance().getService(peerNode, IDebugService.class, false);
			if (dbgService != null) {
				final IProgressMonitor monitor = new NullProgressMonitor();
				IPropertiesContainer props = new PropertiesContainer();
				dbgService.attach(peerNode, props, monitor, new Callback() {
					@Override
	                protected void internalDone(Object caller, IStatus status) {
						// Check if there is a delegate registered
						IDelegate delegate = ServiceUtils.getDelegateServiceDelegate(peerNode, peerNode, IDelegate.class);
						if (delegate != null) {
							delegate.postAttachDebugger(peerNode, monitor, new Callback());
						}
					}
				});
			}
        }

		return null;
	}
}
