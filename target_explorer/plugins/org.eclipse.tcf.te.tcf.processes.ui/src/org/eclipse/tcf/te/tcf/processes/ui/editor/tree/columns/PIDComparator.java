/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.editor.tree.columns;

import java.io.Serializable;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;

/**
 * The comparator for the tree column "PID".
 */
public class PIDComparator implements Comparator<IProcessContextNode>, Serializable {
    private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(final IProcessContextNode o1, final IProcessContextNode o2) {
		final AtomicLong pid1 = new AtomicLong();
		final AtomicLong pid2 = new AtomicLong();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				pid1.set(o1.getSysMonitorContext().getPID());
				pid2.set(o2.getSysMonitorContext().getPID());
			}
		};

		Assert.isTrue(!Protocol.isDispatchThread());
		Protocol.invokeAndWait(runnable);

		return pid1.get() == pid2.get() ? 0 : (pid1.get() < pid2.get() ? -1 : 1);
	}
}
