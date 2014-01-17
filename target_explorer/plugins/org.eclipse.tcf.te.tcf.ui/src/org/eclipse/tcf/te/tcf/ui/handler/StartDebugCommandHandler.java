/**
 * StartDebugCommandHandler.java
 * Created on Jun 29, 2012
 *
 * Copyright (c) 2012, 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
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
import org.eclipse.tcf.te.runtime.services.interfaces.IDebugService;
import org.eclipse.tcf.te.runtime.services.interfaces.IDelegateService;
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
						IDelegateService service = ServiceManager.getInstance().getService(peerNode, IDelegateService.class, false);
						IDelegate delegate = service != null ? service.getDelegate(peerNode, IDelegate.class) : null;

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
