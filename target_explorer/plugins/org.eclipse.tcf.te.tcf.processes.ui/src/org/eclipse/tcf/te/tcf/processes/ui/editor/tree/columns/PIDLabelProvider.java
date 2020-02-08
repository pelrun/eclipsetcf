/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.editor.tree.columns;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ISysMonitor;
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
public class PIDLabelProvider extends AbstractLabelProviderDelegate implements IFontProvider {

	Font pidFont = null;

	public PIDLabelProvider() {
		super();
		FontData fd = new FontData("Courier New", 10, SWT.NORMAL); //$NON-NLS-1$
		pidFont = new Font(Display.getCurrent(), fd );
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();

		if (pidFont != null) pidFont.dispose();
	}

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
			final AtomicReference<BigInteger> pidBI = new AtomicReference<BigInteger>();

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					if (node.getSysMonitorContext() != null) {
						pid.set(node.getSysMonitorContext().getPID());
						if(pid.get() < 0) {
							Object o = node.getSysMonitorContext().getProperties().get(ISysMonitor.PROP_PID);
							if (o instanceof BigInteger) pidBI.set((BigInteger) o);
						}
					}
				}
			};

			Assert.isTrue(!Protocol.isDispatchThread());
			Protocol.invokeAndWait(runnable);

			String id = pid.get() >= 0 ? Long.toString(pid.get()) : ""; //$NON-NLS-1$
			if( pidBI.get() != null && pidBI.get().signum() >= 0 ) {
				id = pidBI.get().toString();
			}
			if (id.startsWith("P")) id = id.substring(1); //$NON-NLS-1$

			IPeerNode peerNode = (IPeerNode)node.getAdapter(IPeerNode.class);
			IProcessMonitorUIDelegate delegate = ServiceUtils.getUIServiceDelegate(peerNode, peerNode, IProcessMonitorUIDelegate.class);
			String newId = delegate != null ? delegate.getText(element, "PID", id) : null; //$NON-NLS-1$
			return newId != null ? newId : id;
		}

        return ""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
	 */
    @Override
    public Font getFont(Object element) {
	    return pidFont;
    }
}
