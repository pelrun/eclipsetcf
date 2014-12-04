/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.editor.tree.columns;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IPendingOperationNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.ui.interfaces.IProcessMonitorUIDelegate;
import org.eclipse.tcf.te.tcf.processes.ui.navigator.runtime.AbstractLabelProviderDelegate;

/**
 * The label provider for the tree column "PID".
 */
public class PIDLabelProvider extends AbstractLabelProviderDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		if (element instanceof IRuntimeModel || element instanceof IPendingOperationNode) {
			return ""; //$NON-NLS-1$
		}

		if (element instanceof IProcessContextNode) {
			final IProcessContextNode node = (IProcessContextNode)element;

			final AtomicLong pid = new AtomicLong();

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					if (node.getSysMonitorContext() != null)
						pid.set(node.getSysMonitorContext().getPID());
				}
			};

			Assert.isTrue(!Protocol.isDispatchThread());
			Protocol.invokeAndWait(runnable);

			String id = pid.get() >= 0 ? Long.toString(pid.get()) : ""; //$NON-NLS-1$
			if (id.startsWith("P")) id = id.substring(1); //$NON-NLS-1$

			IPeerNode peerNode = (IPeerNode)node.getAdapter(IPeerNode.class);
			IProcessMonitorUIDelegate delegate = ServiceUtils.getUIServiceDelegate(peerNode, peerNode, IProcessMonitorUIDelegate.class);
			String newId = delegate != null ? delegate.getText(element, "PID", id) : null; //$NON-NLS-1$
			return newId != null ? newId : id;
		}

        return ""; //$NON-NLS-1$
	}
}
