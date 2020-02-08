/*******************************************************************************
 * Copyright (c) 2014, 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.remote.core.operation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.remote.core.activator.CoreBundleActivator;

public abstract class TCFOperation <R> {
	private IStatus fError;
	private R fResult;
	private boolean fDone;


	protected final boolean shallAbort(IStatus status) {
		synchronized (this) {
			if (fDone)
				return true;
			if (!status.isOK()) {
				setError(status);
				return true;
			}
			return false;
		}
	}

	protected final boolean shallAbort(Throwable error) {
		synchronized (this) {
			if (fDone)
				return true;
			if (error != null) {
				setError(error);
				return true;
			}
			return false;
		}
	}

	protected final void setError(Throwable error) {
		setError(createStatus(error.getMessage(), error));
	}

	protected final Status createStatus(String msg, Throwable error) {
	    return new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), msg, error);
    }

	protected final void setError(IStatus error) {
    	synchronized (this) {
    		fError = error;
    		fDone = true;
    		notifyAll();
		}
    }

	protected void setResult(R result) {
    	synchronized (this) {
    		fResult = result;
    		fDone = true;
    		notifyAll();
		}
    }

    protected R waitForResult(SubMonitor sm) throws CoreException, InterruptedException, OperationCanceledException {
    	synchronized (this) {
    		while (!fDone) {
    			if (sm.isCanceled()) {
    				fDone = true;
    				throw new OperationCanceledException();
    			}
    			wait(1000);
    		}
    		if (fError != null) {
    			throw new CoreException(fError);
    		}
    		return fResult;
    	}
    }

    public final R execute(SubMonitor sm) throws CoreException, OperationCanceledException {
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				doExecute();
			}
		});
		try {
	        return waitForResult(sm);
        } catch (InterruptedException e) {
        	Thread.currentThread().interrupt();
        	throw new OperationCanceledException();
        }
    }

	protected abstract void doExecute();
}
