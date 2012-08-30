/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.internal.columns;

import java.io.Serializable;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;

/**
 * The comparator for the tree column "user".
 */
public class UserComparator implements Comparator<IProcessContextNode> , Serializable {
    private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(final IProcessContextNode o1, final IProcessContextNode o2) {
		final AtomicReference<String> username1 = new AtomicReference<String>();
		final AtomicReference<String> username2 = new AtomicReference<String>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				username1.set(o1.getSysMonitorContext().getUserName());
				username2.set(o2.getSysMonitorContext().getUserName());
			}
		};

		Assert.isTrue(!Protocol.isDispatchThread());
		Protocol.invokeAndWait(runnable);

		if (username1.get() == null) {
			if (username2.get() == null) return 0;
			return -1;
		}
		if (username2.get() == null) return 1;
		return username1.get().compareTo(username2.get());
	}
}
