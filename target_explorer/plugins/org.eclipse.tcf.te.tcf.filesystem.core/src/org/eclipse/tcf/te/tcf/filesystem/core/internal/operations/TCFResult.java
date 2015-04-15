/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.internal.operations;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.utils.StatusHelper;

public class TCFResult<T> {
	private static final long UNRESPONSIVE_TIMEOUT = 60*1000;

	private IStatus fStatus;
	private T fValue;
	private long fCheckedTime;
	private boolean fAutoCancel;

	private List<IProgressMonitor> fMonitors = new ArrayList<IProgressMonitor>();


	public TCFResult() {
		this(true);
	}

	public TCFResult(boolean autoCancel) {
		fAutoCancel = autoCancel;
		resetTimeout();
	}

	public IStatus waitDone(IProgressMonitor monitor) {
		return waitDone(monitor, Long.MAX_VALUE, UNRESPONSIVE_TIMEOUT);
	}

	public IStatus waitDone(IProgressMonitor monitor, long absoluteTimeout) {
		return waitDone(monitor, absoluteTimeout, UNRESPONSIVE_TIMEOUT);
	}

	public synchronized IStatus waitDone(IProgressMonitor monitor, long absoluteTimeout, long unresponsiveTimeout) {
		if (monitor == null)
			monitor = new NullProgressMonitor();

		fMonitors.add(monitor);
		try {
			long startTime = System.currentTimeMillis();
			while (true) {
				if (fStatus != null) {
					return fStatus;
				}

				if (monitor.isCanceled()) {
					return cancelWait(monitor);
				}

				long time = System.currentTimeMillis();
				if (time - startTime > absoluteTimeout || time - fCheckedTime > unresponsiveTimeout) {
					return cancelWait(monitor);
				}

				try {
					wait(100);
				} catch (InterruptedException e) {
					return cancelWait(monitor);
				}
			}
		} finally {
			fMonitors.remove(monitor);
		}
    }

	private IStatus cancelWait(IProgressMonitor monitor) {
		if (fAutoCancel && fMonitors.size() == 1) {
			fStatus = Status.CANCEL_STATUS;
		}

		return Status.CANCEL_STATUS;
	}

	private long resetTimeout() {
		return fCheckedTime = System.currentTimeMillis();
	}

	public T getValue() {
		return fValue;
	}

	public synchronized IStatus setDone(IStatus status, T result) {
		if (fStatus == null) {
			fStatus = status;
			fValue = result;
			notifyAll();
		}
		return fStatus;
	}

	public IStatus setDone(T result) {
		return setDone(Status.OK_STATUS, result);
	}

	public IStatus setError(String msg, Throwable th) {
		return setDone(StatusHelper.createStatus(msg, th), null);
	}

	public IStatus setCancelled() {
		return setDone(Status.CANCEL_STATUS, null);
	}

	public synchronized boolean checkCancelled() {
		if (fStatus != null) {
			return true;
		}
		resetTimeout();
		return false;
	}

	public IStatus getStatus() {
		return fStatus;
	}

	public synchronized boolean hasWaiters() {
		return !fMonitors.isEmpty();
	}
}
