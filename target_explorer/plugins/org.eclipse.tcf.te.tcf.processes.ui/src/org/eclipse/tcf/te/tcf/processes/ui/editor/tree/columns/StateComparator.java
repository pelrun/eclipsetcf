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
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;

/**
 * The comparator for the tree column "state".
 */
public class StateComparator implements Comparator<IProcessContextNode> , Serializable {
    private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(final IProcessContextNode o1, final IProcessContextNode o2) {
		final AtomicReference<String> state1 = new AtomicReference<String>();
		final AtomicReference<String> state2 = new AtomicReference<String>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				state1.set(o1.getSysMonitorContext().getState());
				state2.set(o2.getSysMonitorContext().getState());
			}
		};

		Assert.isTrue(!Protocol.isDispatchThread());
		Protocol.invokeAndWait(runnable);

		if (state1.get() == null) {
			if (state2.get() == null) return 0;
			return -1;
		}
		if (state2.get() == null) return 1;
		return state1.get().compareTo(state2.get());
	}
}
