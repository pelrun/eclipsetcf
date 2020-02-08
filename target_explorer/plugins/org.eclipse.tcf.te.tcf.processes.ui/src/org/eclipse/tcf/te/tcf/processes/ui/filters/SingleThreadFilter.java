/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.filters;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ISysMonitor;
import org.eclipse.tcf.services.ISysMonitor.SysMonitorContext;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;

/**
 * The filter to filter out the single thread of a process which has the same name and id with its
 * parent process.
 */
public class SingleThreadFilter extends ViewerFilter {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(final Viewer viewer, Object parentElement, Object element) {
		if (parentElement instanceof TreePath) {
			parentElement = ((TreePath) parentElement).getLastSegment();
		}
		if (parentElement instanceof IProcessContextNode && element instanceof IProcessContextNode) {
			final AtomicBoolean selected = new AtomicBoolean(true);
			final Object pe = parentElement;
			final Object e = element;

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					IProcessContextNode parent = (IProcessContextNode)pe;
					IProcessContextNode child = (IProcessContextNode)e;
					if (parent.getChildren().length == 1) {
						if (parent.getSysMonitorContext() != null && child.getSysMonitorContext() != null &&
										parent.getSysMonitorContext().getPID() == child.getSysMonitorContext().getPID()) {
							SysMonitorContext smc = parent.getSysMonitorContext();
							if (Integer.valueOf(ISysMonitor.EXETYPE_KERNEL).equals(smc.getProperties().get(ISysMonitor.PROP_EXETYPE))) {
								selected.set(false);
							} else if (parent.getName() != null) {
								selected.set(!parent.getName().equals(child.getName()));
							}
							else if (child.getName() != null) {
								selected.set(!child.getName().equals(parent.getName()));
							}
						}
					}
				}
			};

			Assert.isTrue(!Protocol.isDispatchThread());
			Protocol.invokeAndWait(runnable);

			return selected.get();
		}
		return true;
	}
}
