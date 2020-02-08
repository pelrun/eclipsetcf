/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.core.steps;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.core.utils.ConnectStateHelper;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep;
import org.eclipse.tcf.te.runtime.utils.ProgressHelper;

/**
 * SetConnectStateStep
 */
public class SetConnectStateStep extends AbstractStep {

	public static final String PARAMETER_STATE = "state"; //$NON-NLS-1$
	public static final String PARAMETER_STATE_ON_CANCEL = "stateOnCancel"; //$NON-NLS-1$
	public static final String PARAMETER_STATE_ON_ERROR = "stateOnError"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor, ICallback callback) {
		String state = getParameters().get(PARAMETER_STATE);
		if (state != null) {
			Object activeContext = getActiveContext(context, data, fullQualifiedId);
			if (activeContext instanceof IConnectable) {
				((IConnectable)activeContext).setConnectState(ConnectStateHelper.getConnectState(state));
			}
		}

		callback.done(this, Status.OK_STATUS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep#rollback(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.core.runtime.IStatus, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void rollback(IStepContext context, IPropertiesContainer data, IStatus status, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor, ICallback callback) {
		if (ProgressHelper.isCancel(this, monitor, null)) {
			String stateOnCancel = getParameters().get(PARAMETER_STATE_ON_CANCEL);

			if (stateOnCancel != null) {
				Object activeContext = getActiveContext(context, data, fullQualifiedId);
				if (activeContext instanceof IConnectable) {
					((IConnectable)activeContext).setConnectState(ConnectStateHelper.getConnectState(stateOnCancel));
				}
			}
		}
		else {
			String stateOnError = getParameters().get(PARAMETER_STATE_ON_ERROR);

			if (stateOnError != null) {
				Object activeContext = getActiveContext(context, data, fullQualifiedId);
				if (activeContext instanceof IConnectable) {
					((IConnectable)activeContext).setConnectState(ConnectStateHelper.getConnectState(stateOnError));
				}
			}
		}

		super.rollback(context, data, status, fullQualifiedId, monitor, callback);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
	}

}
