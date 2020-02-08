/*******************************************************************************
 * Copyright (c) 2015, 2016 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.concurrent;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.tcf.core.activator.CoreBundleActivator;

/**
 * Utility class that governs the execution of an asynchronous operation. The class is agnostic on how
 * the operation is actually carried out.
 */
public class TCFOperationMonitor<T> {
	private static final long UNRESPONSIVE_TIMEOUT = 60*1000;

	private IStatus fStatus;
	private T fValue;
	private long fCheckedTime;
	private boolean fPropagateCancel;

	private List<IProgressMonitor> fProgressMonitors = new ArrayList<IProgressMonitor>();

	/**
	 * Create a new operation monitor.
	 */
	public TCFOperationMonitor() {
		this(true);
	}

	/**
	 * @param propagateProgressMonitorCancel whether to cancel the operation when all the progress monitors
	 * of the waiting callers are canceled.
	 * @see #waitDone(IProgressMonitor)
	 * @see #waitDone(IProgressMonitor, long)
	 * @see #waitDone(IProgressMonitor, long, long)
	 */
	public TCFOperationMonitor(boolean propagateProgressMonitorCancel) {
		fPropagateCancel = propagateProgressMonitorCancel;
		fCheckedTime = System.currentTimeMillis();
	}

	/**
	 * Wait for the operation to finish, using default timeouts.
	 */
	public IStatus waitDone(IProgressMonitor monitor) {
		return waitDone(monitor, Long.MAX_VALUE, UNRESPONSIVE_TIMEOUT);
	}

	/**
	 * Wait for the operation to finish, using the specified timeout.
	 * @param timeoutms maximum time to wait for the result to be set. When this timeout expires
	 * {@link Status#CANCEL_STATUS} is returned.
	 */
	public IStatus waitDone(IProgressMonitor monitor, long timeoutms) {
		return waitDone(monitor, timeoutms, UNRESPONSIVE_TIMEOUT);
	}

	/**
	 * Wait for the operation to finish, using the specified timeouts.
	 * @param timeoutms maximum time to wait for the result to be set. When this timeout expires
	 * {@link Status#CANCEL_STATUS} is returned.
	 * @param unresponsiveTimeoutms maximum time the executing task is allowed not to check for
	 * cancellation. When this timeout expires {@link Status#CANCEL_STATUS} is returned.
	 */
	public synchronized IStatus waitDone(IProgressMonitor monitor, long timeoutms, long unresponsiveTimeoutms) {
		if (monitor == null)
			monitor = new NullProgressMonitor();

		fProgressMonitors.add(monitor);
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
				if (time - startTime > timeoutms || time - fCheckedTime > unresponsiveTimeoutms) {
					return cancelWait(monitor);
				}

				try {
					wait(100);
				} catch (InterruptedException e) {
					return cancelWait(monitor);
				}
			}
		} finally {
			fProgressMonitors.remove(monitor);
		}
    }

	private IStatus cancelWait(IProgressMonitor monitor) {
		if (fPropagateCancel && fProgressMonitors.size() == 1) {
			fStatus = Status.CANCEL_STATUS;
		}

		return Status.CANCEL_STATUS;
	}

	/**
	 * Returns the status of the operation, or <code>null</code> when the operation is still in progress.
	 */
	public IStatus getStatus() {
		return fStatus;
	}

	/**
	 * Returns the value of this result object, or <code>null</code> when it was not set.
	 */
	public T getValue() {
		return fValue;
	}

	/**
	 * Return whether a thread is waiting for the completion of the operation.
	 */
	public synchronized boolean hasWaiters() {
		return !fProgressMonitors.isEmpty();
	}

	/**
	 * Method for reporting the completion of the operation.
	 */
	public synchronized IStatus setDone(IStatus status, T result) {
		if (fStatus == null) {
			fStatus = status;
			fValue = result;
			notifyAll();
		}
		return fStatus;
	}

	/**
	 * Method for reporting the successful completion of the operation.
	 */
	public IStatus setDone(T result) {
		return setDone(Status.OK_STATUS, result);
	}

	/**
	 * Method for reporting the completion of the operation with an error.
	 */
	public IStatus setError(String msg, Throwable th) {
		return setDone(createStatus(msg, th), null);
	}

	/**
	 * Method for reporting the completion of the operation with an error.
	 */
	public IStatus setError(IStatus status) {
		return setDone(status, null);
	}

	private IStatus createStatus(String msg, Throwable th) {
		if (th != null) {
			String msg2= th.getLocalizedMessage();
			if (msg2 != null) {
				msg = msg == null ? msg2 : msg + ": " + msg2; //$NON-NLS-1$
			}
		}
		return new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), msg, th);
    }

	/**
	 * Method for reporting cancellation of the operation.
	 */
	public IStatus setCancelled() {
		return setDone(Status.CANCEL_STATUS, null);
	}

	/**
	 * Method for checking whether the operation has been cancelled. Calling this method indicates
	 * that the operation is still responsive.
	 */
	public synchronized boolean checkCancelled() {
		if (fStatus != null) {
			return true;
		}
		fCheckedTime = System.currentTimeMillis();
		return false;
	}
}
