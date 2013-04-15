/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.stepper.job;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IPropertiesAccessService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepper;
import org.eclipse.tcf.te.runtime.stepper.stepper.Stepper;

/**
 * Stepper job implementation.
 */
public class StepperJob extends Job {

	final private IStepContext stepContext;
	final private IPropertiesContainer data;
	final private String stepGroupId;
	final private String operation;

	private ICallback jobCallback = null;
	private boolean isFinished = false;
	private boolean isCanceled = false;

	/**
	 * Constructor.
	 *
	 * @param name The job name.
	 * @param stepContext The step context.
	 * @param data The stepper data.
	 * @param stepGroupId The step group id to execute.
	 * @param operation The operation to execute.
	 */
	public StepperJob(String name, IStepContext stepContext, IPropertiesContainer data, String stepGroupId, String operation) {
		super(name);
		setPriority(Job.INTERACTIVE);

		Assert.isNotNull(stepContext);
		Assert.isNotNull(data);
		Assert.isNotNull(stepGroupId);
		Assert.isNotNull(operation);

		this.stepContext = stepContext;
		this.data = data;
		this.stepGroupId = stepGroupId;
		this.operation = operation;

		if (stepContext.getContextObject() instanceof ISchedulingRule) {
			setRule((ISchedulingRule)stepContext.getContextObject());
		}

		IPropertiesAccessService service = ServiceManager.getInstance().getService(stepContext.getContextObject(), IPropertiesAccessService.class);
		StepperJob job = service != null ? (StepperJob)service.getProperty(stepContext.getContextObject(), StepperJob.class.getName() + "." + operation) : null; //$NON-NLS-1$
		if (service == null && stepContext.getContextObject() instanceof IPropertiesContainer)
			job = (StepperJob)((IPropertiesContainer)stepContext.getContextObject()).getProperty(StepperJob.class.getName() + "." + operation); //$NON-NLS-1$
		Assert.isTrue(job == null);

		if (service != null)
			service.setProperty(stepContext.getContextObject(), StepperJob.class.getName() + "." + operation, this); //$NON-NLS-1$
		else if (stepContext.getContextObject() instanceof IPropertiesContainer)
			((IPropertiesContainer)stepContext.getContextObject()).setProperty(StepperJob.class.getName() + "." + operation, this); //$NON-NLS-1$
	}

	/**
	 * Set the callback for the job.
	 * @param callback The callback.
	 */
	public final void setJobCallback(ICallback callback) {
		jobCallback = callback;
	}

	/**
	 * Return the job callback.
	 */
	public final ICallback getJobCallback() {
		return jobCallback;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected final IStatus run(IProgressMonitor monitor) {

		// The stepper instance to be used
		IStepper stepper = new Stepper(getName());
		IStatus status = Status.OK_STATUS;

		try {
			// Initialize stepper
			stepper.initialize(stepContext, stepGroupId, data, monitor);

			// Execute stepper
			stepper.execute();
		} catch (CoreException e) {
			status = e.getStatus();
		} finally {
			// Cleanup the stepper
			stepper.cleanup();

			IPropertiesAccessService service = ServiceManager.getInstance().getService(stepContext.getContextObject(), IPropertiesAccessService.class);
			if (service != null)
				service.setProperty(stepContext.getContextObject(), StepperJob.class.getName() + "." + operation, null); //$NON-NLS-1$
			else if (stepContext.getContextObject() instanceof IPropertiesContainer)
				((IPropertiesContainer)stepContext.getContextObject()).setProperty(StepperJob.class.getName() + "." + operation, null); //$NON-NLS-1$
		}

		if (jobCallback != null)
			jobCallback.done(this, status);

		isFinished = true;

		return status;
	}

	public boolean isFinished() {
		return isFinished;
	}

	public boolean isCanceled() {
		return isCanceled;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#canceling()
	 */
	@Override
	protected void canceling() {
	    super.canceling();
	    isCanceled = true;
	}
}
