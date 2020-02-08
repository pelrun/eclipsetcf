/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.concurrent;

import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.tcf.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.core.nls.Messages;

/**
 * A timer task used by CallbackMonitor to monitor timeout waiting.
 */
class MonitorTask extends TimerTask {
	// The timeout
	private long timeout;
	// The initial time.
	private long startTime;
	// The callback which is invoked after timeout.
	private ICallback callback;
	
	/**
	 * The constructor.
	 * 
	 * @param callback
	 * @param timeout
	 */
	public MonitorTask(ICallback callback, long timeout) {
		this.callback = callback;
		this.timeout = timeout;
		this.startTime = System.currentTimeMillis();
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	@Override
	public void run() {
		synchronized (callback) {
			if (callback.isDone()) {
				cancel();
			}
			else if (System.currentTimeMillis() - startTime >= timeout) {
				cancel();
				IStatus status = new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), 
								Messages.MonitorTask_TimeoutError, new TimeoutException());
				callback.done(this, status);
			}
		}
	}
}
