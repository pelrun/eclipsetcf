/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.steps;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;
import org.eclipse.tcf.te.tcf.locator.activator.CoreBundleActivator;

/**
 * Check service step implementation.
 */
public class CheckServiceStep extends AbstractPeerNodeStep {

	public static final String PARAMETER_REMOTE_SERVICE = "remoteService"; //$NON-NLS-1$
	public static final String PARAMETER_LOCAL_SERVICE = "localService"; //$NON-NLS-1$
	public static final String PARAMETER_OFFLINE_SERVICE = "offlineService"; //$NON-NLS-1$

	/**
	 * Constructor.
	 */
	public CheckServiceStep() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IExtendedStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(final IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor) throws CoreException {
		if (Protocol.isDispatchThread()) {
			internalValidateExecute(context, data, fullQualifiedId, monitor);
		}
		else {
			final AtomicReference<CoreException> result = new AtomicReference<CoreException>();
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					try {
						internalValidateExecute(context, data, fullQualifiedId, monitor);
					}
					catch (CoreException e) {
						result.set(e);
					}
				}
			});
			if (result.get() != null) {
				throw result.get();
			}
		}
	}

	protected void internalValidateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);
		if (channel == null || channel.getState() != IChannel.STATE_OPEN) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "missing or closed channel")); //$NON-NLS-1$
		}

		String remoteService = getParameters().get(PARAMETER_REMOTE_SERVICE);
		if (remoteService != null && channel.getRemoteService(remoteService) == null) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "missing remote service '" + remoteService + "'")); //$NON-NLS-1$ //$NON-NLS-2$
		}

		String localService = getParameters().get(PARAMETER_LOCAL_SERVICE);
		if (localService != null && channel.getLocalService(localService) == null) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "missing local service '" + localService + "'")); //$NON-NLS-1$ //$NON-NLS-2$
		}

		String offlineService = getParameters().get(PARAMETER_OFFLINE_SERVICE);
		String services = getActivePeerContext(context, data, fullQualifiedId).getAttributes().get(IPeerProperties.PROP_OFFLINE_SERVICES);
		List<String> list = services != null ? Arrays.asList(services.split(",\\s*")) : Collections.EMPTY_LIST; //$NON-NLS-1$
		if (offlineService != null && !list.contains(offlineService)) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "missing offline service '" + offlineService + "'")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(final IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, final ICallback callback) {
		// nothing to do, check is done in validateExecute.
		callback.done(this, Status.OK_STATUS);
	}
}
