/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.statushandler.StatusHandlerManager;
import org.eclipse.tcf.te.runtime.statushandler.interfaces.IStatusHandler;
import org.eclipse.tcf.te.runtime.statushandler.interfaces.IStatusHandlerConstants;
import org.eclipse.tcf.te.tcf.core.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.core.nls.Messages;

/**
 * Abstract TCF job implementation.
 * <p>
 * The job implementation assures that the job logic is executed within the
 * TCF event dispatch thread. The job passes on the execution status to a
 * registered status handler if no parent callback is set.
 */
public abstract class AbstractJob extends Job {
	// The parent callback.
	/* default */ final ICallback parentCallback;

	/**
	 * Constructor.
	 *
	 * @param name The job name. Must not be <code>null</code>.
	 */
    public AbstractJob(String name) {
	    this(name, null);
    }

	/**
	 * Constructor.
	 *
	 * @param name The job name. Must not be <code>null</code>.
	 * @param parentCallback The parent callback or <code>null</code>.
	 */
    public AbstractJob(String name, ICallback parentCallback) {
	    super(name);
	    this.parentCallback = parentCallback;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected final IStatus run(final IProgressMonitor monitor) {
		// The first runnable is setting the thread which will finish the job at the end
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				AbstractJob.this.setThread(Thread.currentThread());
			}
		});

		final IProgressMonitor finMonitor = monitor;

		// The callback to invoke from the job's logic must be created before scheduling
		// the job logic asynchronously
		final ICallback callback = new Callback() {
			@Override
			protected void internalDone(final Object caller, final IStatus status) {
				// If the parent callback is not set, pass on the status to a registered status handler
				if (parentCallback == null) {
					// If we have a non-OK or CANCEL status, forward to the status handler
					if (status != null && status.getSeverity() != IStatus.CANCEL && status.getSeverity() != IStatus.OK) {
						IStatusHandler[] handler = StatusHandlerManager.getInstance().getHandler(AbstractJob.this);
						if (handler.length > 0) {
							IPropertiesContainer data = new PropertiesContainer();
							data.setProperty(IStatusHandlerConstants.PROPERTY_TITLE, getProperty(IStatusHandlerConstants.PROPERTY_TITLE) != null ? getProperty(IStatusHandlerConstants.PROPERTY_TITLE) : Messages.AbstractJob_error_dialogTitle);
							data.setProperty(IStatusHandlerConstants.PROPERTY_CONTEXT_HELP_ID, getProperty(IStatusHandlerConstants.PROPERTY_CONTEXT_HELP_ID) != null ? getProperty(IStatusHandlerConstants.PROPERTY_CONTEXT_HELP_ID) : IContextHelpIds.MESSAGE_OPERATION_FAILED);
							data.setProperty(IStatusHandlerConstants.PROPERTY_CALLER, AbstractJob.this);

							handler[0].handleStatus(status, data, null);
						}
					}
				} else {
					// Pass on the caller and the status to the parent callback
					// We call the parent callback asynchronously in the TCF
					// event dispatch thread. The job termination happens in
					// a separate runnable.
					Protocol.invokeLater(new Runnable() {
	                    @Override
                        public void run() {
		                    parentCallback.done(caller, status);
	                    }
                    });
				}

				// Job termination must must happen in the TCF event dispatch thread as
				// this is the thread we've set before as the termination thread.
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						// Mark the job as done
						IStatus result = finMonitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
						AbstractJob.this.done(result);
					}
				};

				if (Protocol.isDispatchThread()) runnable.run();
				else Protocol.invokeLater(runnable);
			}
		};

		// Run the job logic
		Protocol.invokeLater(new Runnable() {
			@Override
			public void run() {
				AbstractJob.this.run(monitor, callback);
			}
		});

        return ASYNC_FINISH;
    }

    /**
     * Executes the job logic.
     *
     * @param monitor The progress monitor. Must not be <code>null</code>.
     * @param callback The callback to invoke. Must not be <code>null</code>.
     */
    public abstract void run(IProgressMonitor monitor, ICallback callback);
}
