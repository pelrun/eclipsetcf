/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.steps;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.services.IProcessesV1;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.services.IRunControl.RunControlContext;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.utils.ProgressHelper;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;
import org.eclipse.tcf.te.tcf.core.steps.AbstractPeerStep;
import org.eclipse.tcf.te.tcf.processes.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.steps.IProcessesStepAttributes;

/**
 * Resume context step implementation.
 */
public class ResumeProcessStep extends AbstractPeerStep {

	/**
	 * Constructor.
	 */
	public ResumeProcessStep() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IExtendedStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);
		if (channel == null || channel.getState() != IChannel.STATE_OPEN) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "missing or closed channel")); //$NON-NLS-1$
		}

		IProcesses.ProcessContext processContext = (IProcesses.ProcessContext)StepperAttributeUtil.getProperty(IProcessesStepAttributes.ATTR_PROCESS_CONTEXT, fullQualifiedId, data);
		if (processContext == null) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "missing process context")); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(final IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, final ICallback callback) {
		if (Protocol.isDispatchThread()) {
			internalExecute(context, data, fullQualifiedId, monitor, callback);
		}
		else {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					internalExecute(context, data, fullQualifiedId, monitor, callback);
				}
			});
		}
	}

	protected void internalExecute(IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, final ICallback callback) {
		final IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);
		final IProcesses.ProcessContext processContext = (IProcesses.ProcessContext)StepperAttributeUtil.getProperty(IProcessesStepAttributes.ATTR_PROCESS_CONTEXT, fullQualifiedId, data);
		final IRunControl runControl = channel.getRemoteService(IRunControl.class);
		final String svcProcessesName = (String)StepperAttributeUtil.getProperty("services.processes.name", fullQualifiedId, data); //$NON-NLS-1$

		if (IProcessesV1.NAME.equals(svcProcessesName)) {
			// If the processes service used is IProcessesV1, there is nothing to do here
			callback.done(ResumeProcessStep.this, Status.OK_STATUS);
		} else if (runControl != null) {
			boolean stopAtEntry = StepperAttributeUtil.getBooleanProperty(IProcessesStepAttributes.ATTR_STOP_AT_ENTRY, fullQualifiedId, data);

			// In case "stop at entry" is not desired, we have to resume the context once to
			// "stop at main".
			if (!stopAtEntry) {
				runControl.getContext(processContext.getID(), new IRunControl.DoneGetContext() {
					@Override
					public void doneGetContext(IToken token, Exception error, RunControlContext context) {
						ProgressHelper.worked(monitor, 5);
						if (!ProgressHelper.isCancelOrError(ResumeProcessStep.this, StatusHelper.getStatus(error), monitor, callback)) {
							context.resume(IRunControl.RM_RESUME, 1, new IRunControl.DoneCommand() {
								@Override
								public void doneCommand(IToken token, Exception error) {
									callback.done(ResumeProcessStep.this, StatusHelper.getStatus(error));
								}
							});
						}
					}
				});
			} else {
				callback.done(ResumeProcessStep.this, Status.OK_STATUS);
			}
		}
		else {
			callback.done(this, new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "missing run control service")); //$NON-NLS-1$
		}
	}
}
