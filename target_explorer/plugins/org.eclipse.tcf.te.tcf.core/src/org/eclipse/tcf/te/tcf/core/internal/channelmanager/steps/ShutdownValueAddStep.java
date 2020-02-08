/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.core.internal.channelmanager.steps;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.tcf.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;
import org.eclipse.tcf.te.tcf.core.steps.AbstractPeerStep;
import org.eclipse.tcf.te.tcf.core.va.interfaces.IValueAdd;

/**
 * ShutdownValueAddStep
 */
public class ShutdownValueAddStep extends AbstractPeerStep {
	/**
	 * If set to <code>true</code>, skip the shutdown step.
	 */
	public static final String PROP_SKIP_SHUTDOWN_STEP = "ShutdownValueAddStep.skip"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(context);
		Assert.isNotNull(data);
		Assert.isNotNull(fullQualifiedId);
		Assert.isNotNull(monitor);

		IValueAdd valueAdd = (IValueAdd)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_VALUE_ADD, fullQualifiedId, data);
		if (valueAdd == null) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "Value-add descriptor instance not set.")); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(final IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, final ICallback callback) {
		Assert.isNotNull(context);
		Assert.isNotNull(data);
		Assert.isNotNull(fullQualifiedId);
		Assert.isNotNull(monitor);
		Assert.isNotNull(callback);

		final IValueAdd valueAdd = (IValueAdd)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_VALUE_ADD, fullQualifiedId, data);
		final String peerId = getActivePeerContext(context, data, fullQualifiedId).getID();

		boolean skip = StepperAttributeUtil.getBooleanProperty(PROP_SKIP_SHUTDOWN_STEP, fullQualifiedId, data);
		if (!skip) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					valueAdd.shutdown(peerId, callback);
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeLater(runnable);
		} else {
			callback(data, fullQualifiedId, callback, Status.OK_STATUS, null);
		}
	}
}
