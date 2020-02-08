/*******************************************************************************
 * Copyright (c) 2013, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.runtime.stepper.steps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.tcf.te.runtime.callback.AsyncCallbackCollector;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepAttributes;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.job.StepperJob;
import org.eclipse.tcf.te.runtime.utils.ProgressHelper;

/**
 * Cancel als StepperJobs jobs.
 */
public class CancelJobsStep extends AbstractStep {

	/**
	 * Constructor.
	 */
	public CancelJobsStep() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(final IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, final ICallback callback) {
		Job thisJob = (StepperJob)StepperAttributeUtil.getProperty(IStepAttributes.ATTR_STEPPER_JOB, fullQualifiedId, data);

		final AsyncCallbackCollector collector = new AsyncCallbackCollector();
		Map<String,List<Job>> jobs = new HashMap<String, List<Job>>(StepperJob.getJobs(context.getContextObject()));
		final AtomicInteger canceledJobs = new AtomicInteger(0);
		final List<Job> jobsToCancel = new ArrayList<Job>();
		synchronized (jobs) {
			for (String op : jobs.keySet()) {
				for (Job job : jobs.get(op)) {
					if (job != thisJob &&
									(!(job instanceof StepperJob) || ((StepperJob)job).isCancelable())) {
						jobsToCancel.add(job);
					}
				}
			}
		}
		for (Job job : jobsToCancel) {
			if (job instanceof StepperJob && ((StepperJob)job).isCancelable()) {
				Callback jobCb = new Callback(((StepperJob)job).getJobCallback()) {
					@Override
					protected void internalDone(Object caller, IStatus status) {
						canceledJobs.incrementAndGet();
						ProgressHelper.worked(monitor, getTotalWork(context, data) / jobsToCancel.size());
						ProgressHelper.setSubTaskName(monitor, canceledJobs.get() + " of " + jobsToCancel.size() + " Jobs canceled."); //$NON-NLS-1$ //$NON-NLS-2$
						collector.removeCallback(this);
					}
				};
				if (job.getState() == Job.RUNNING) {
					collector.addCallback(jobCb);
					((StepperJob)job).setJobCallback(jobCb);
				}
				else {
					canceledJobs.incrementAndGet();
				}
			}
			else {
				canceledJobs.incrementAndGet();
			}
			job.cancel();
		}

		collector.initDone();

		ExecutorsUtil.waitAndExecute(0, collector.getConditionTester());

		callback.done(this, Status.OK_STATUS);
	}
}
