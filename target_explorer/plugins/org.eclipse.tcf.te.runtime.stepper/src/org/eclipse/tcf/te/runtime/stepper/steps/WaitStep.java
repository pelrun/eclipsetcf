/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.runtime.stepper.steps;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.interfaces.IConditionTester;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;

/**
 * Step to wait some time.
 * The timeout is 1000ms per default.
 * A specific timeout can be set through parameter "timeout" for a step group reference.
 */
public class WaitStep extends AbstractStep {

	/**
	 * Constructor.
	 */
	public WaitStep() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, ICallback callback) {
		int timeout = 1000;
		if (getParameters() != null) {
			String value = getParameters().get("timeout"); //$NON-NLS-1$
			if (value != null) {
				try {
					timeout = Integer.parseInt(value);
				}
				catch (Exception e) {
				}
			}
		}

		ExecutorsUtil.waitAndExecute(timeout, new IConditionTester() {
			@Override
			public boolean isConditionFulfilled() {
				return monitor.isCanceled();
			}

			@Override
			public void cleanup() {
			}
		});
		callback.done(this, monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep#getCancelTimeout()
	 */
	@Override
	public int getCancelTimeout() {
		int timeout = 1000;
		if (getParameters() != null) {
			String value = getParameters().get("timeout"); //$NON-NLS-1$
			if (value != null) {
				try {
					timeout = Integer.parseInt(value);
				}
				catch (Exception e) {
				}
			}
		}

		return Math.max(super.getCancelTimeout(), timeout + 10000);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
	}
}
