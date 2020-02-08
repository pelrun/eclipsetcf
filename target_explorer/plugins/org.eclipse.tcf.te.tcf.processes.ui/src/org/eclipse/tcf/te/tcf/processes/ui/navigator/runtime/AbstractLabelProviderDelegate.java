/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.navigator.runtime;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.ui.PlatformUI;

/**
 * Abstract label provider delegate implementation.
 */
public abstract class AbstractLabelProviderDelegate extends LabelProvider implements IColorProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	@Override
	public Color getForeground(Object element) {
		if (element instanceof IProcessContextNode) {
			final IProcessContextNode node = (IProcessContextNode) element;
			final AtomicBoolean canAttach = new AtomicBoolean();

			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					if (node.getProcessContext() != null) {
						if (node.getProcessContext().getProperties().containsKey("CanAttach")) { //$NON-NLS-1$
							Boolean value = (Boolean)node.getProcessContext().getProperties().get("CanAttach"); //$NON-NLS-1$
							canAttach.set(value != null && value.booleanValue());
						} else {
							canAttach.set(true);
						}
					}
				}
			};
			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);

			if (!canAttach.get()) {
				return PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
			}
		}

	    return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
	 */
	@Override
	public Color getBackground(Object element) {
	    return null;
	}
}
