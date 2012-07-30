/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.filesystem.callbacks;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.callbacks.QueryDoneOpenChannel;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.callbacks.RefreshStateDoneOpenChannel;
import org.eclipse.tcf.te.tests.tcf.filesystem.FSPeerTestCase;

public class QueryChildrenCallbackTest extends FSPeerTestCase {
	public void testQueryChildren() throws Exception {
		Assert.isNotNull(testRoot);
		testRoot.childrenQueryRunning = true;
		final AtomicReference<IStatus> statusRef = new AtomicReference<IStatus>();
		final Callback callback = new Callback(){
			@Override
            protected void internalDone(Object caller, IStatus status) {
				statusRef.set(status != null ? status : Status.OK_STATUS);
            }
		};
		Tcf.getChannelManager().openChannel(peer, null, new QueryDoneOpenChannel(testRoot,callback));
		waitAndDispatch(0, callback.getDoneConditionTester(new NullProgressMonitor()));
		assertTrue(statusRef.get().isOK());
	}
	public void testRefreshState() throws Exception {
		Assert.isNotNull(testFile);
		testFile.childrenQueryRunning = true;
		final AtomicReference<IStatus> statusRef = new AtomicReference<IStatus>();
		final Callback callback = new Callback(){
			@Override
            protected void internalDone(Object caller, IStatus status) {
				statusRef.set(status != null ? status : Status.OK_STATUS);
            }
		};
		Tcf.getChannelManager().openChannel(peer, null, new RefreshStateDoneOpenChannel(testFile, callback));
		waitAndDispatch(0, callback.getDoneConditionTester(new NullProgressMonitor()));
		assertTrue(statusRef.get().isOK());
	}
}
