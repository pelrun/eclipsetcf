/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.launch.core.steps;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.tcf.te.launch.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep;

/**
 * Abstract launch step implementation.
 */
public abstract class AbstractLaunchStep extends AbstractStep {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IExtendedStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		if (getLaunch(context) == null) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "missing launch context")); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the launch object for the given step context.
	 *
	 * @param context The step context.
	 * @return The launch or <code>null</code>.
	 */
	protected ILaunch getLaunch(IStepContext context) {
		Assert.isNotNull(context);
		return (ILaunch)context.getAdapter(ILaunch.class);
	}

	/**
	 * Returns the launch configuration for the given step context.
	 *
	 * @param context The step context.
	 * @return The launch configuration or <code>null</code>.
	 */
	protected ILaunchConfiguration getLaunchConfiguration(IStepContext context) {
		Assert.isNotNull(context);
		return (ILaunchConfiguration)context.getAdapter(ILaunchConfiguration.class);
	}

	/**
	 * Returns the launch configuration type for the given step context.
	 *
	 * @param context The step context.
	 * @return The launch configuration type or <code>null</code>.
	 */
	protected ILaunchConfigurationType getLaunchConfigurationType(IStepContext context) {
		Assert.isNotNull(context);
		return (ILaunchConfigurationType)context.getAdapter(ILaunchConfigurationType.class);
	}

	/**
	 * Returns the current launch mode.
	 *
	 * @param context The step context.
	 * @return The launch mode or <code>null</code>.
	 */
	protected String getLaunchMode(IStepContext context) {
		ILaunch launch = getLaunch(context);
		return launch != null ? launch.getLaunchMode() : null;
	}

	/**
	 * Returns the active model node context that is currently used.
	 *
	 * @param context The step context. Must not be <code>null</code>.
	 * @param data The data giving object. Must not be <code>null</code>.
	 * @param fullQualifiedId The full qualified id for this step. Must not be <code>null</code>.
	 * @return The active model node context.
	 */
	protected IModelNode getActiveModelNodeContext(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId) {
		Object activeContext = getActiveContext(context, data, fullQualifiedId);
		IModelNode modelNode = null;
		if (activeContext instanceof IModelNode)
			return (IModelNode)activeContext;
		if (activeContext instanceof IAdaptable)
			modelNode = (IModelNode)((IAdaptable)activeContext).getAdapter(IModelNode.class);
		if (modelNode == null)
			modelNode = (IModelNode)Platform.getAdapterManager().getAdapter(activeContext, IModelNode.class);

		return modelNode;
	}
}
