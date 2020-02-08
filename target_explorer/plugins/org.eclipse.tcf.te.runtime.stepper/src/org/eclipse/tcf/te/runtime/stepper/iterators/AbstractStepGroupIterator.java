/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.runtime.stepper.iterators;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.runtime.extensions.ExecutableExtension;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.activator.CoreBundleActivator;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepAttributes;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupIterator;

/**
 * Abstract step group iterator.
 */
public abstract class AbstractStepGroupIterator extends ExecutableExtension implements IStepGroupIterator {

	private int iteration = -1;
	private int iterations = -1;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupIterator#initialize(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void initialize(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		iteration = 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupIterator#hasNext(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public final boolean hasNext(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		return iteration < iterations;
	}

	/**
	 * Set the nuumber of iterations.
	 * @param iterations The number of iterations.
	 */
	protected final void setIterations(int iterations) throws CoreException {
		if (iteration > 0) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "cannot change iterations during run")); //$NON-NLS-1$
		}
		this.iterations = iterations;
	}

	/**
	 * Return the current iteration index.
	 * @return The iteration index.
	 */
	protected final int getIteration() {
		return iteration;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupIterator#next(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public final void next(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		if (iterations < 0 || iteration < 0) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "iterator not initialized")); //$NON-NLS-1$
		}
		if (iteration >= iterations) {
			throw new CoreException(new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), "no more iterations")); //$NON-NLS-1$
		}

		internalNext(context, data, fullQualifiedId, monitor);

		iteration++;
	}

	/**
	 * Set the next iteration to the data using the full qualified id.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param data The data. Must not be <code>null</code>.
	 * @param fullQualifiedId The full qualified id for this step. Must not be <code>null</code>.
	 * @param monitor The progress monitor. Must not be <code>null</code>.
	 * @throws CoreException
	 */
	public abstract void internalNext(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException;

	/**
	 * Set the active context.
	 *
	 * @param activeContext The new active context.
	 * @param data The data giving object. Must not be <code>null</code>.
	 * @param fullQualifiedId The full qualified id for this step. Must not be <code>null</code>.
	 */
	protected void setActiveContext(Object activeContext, IPropertiesContainer data, IFullQualifiedId fullQualifiedId) {
		Assert.isNotNull(data);
		Assert.isNotNull(fullQualifiedId);
		StepperAttributeUtil.setProperty(IStepAttributes.ATTR_ACTIVE_CONTEXT, fullQualifiedId, data, activeContext);
	}

	/**
	 * Returns the active context that is currently used.
	 *
	 * @param context The step context. Must not be <code>null</code>.
	 * @param data The data giving object. Must not be <code>null</code>.
	 * @param fullQualifiedId The full qualified id for this step. Must not be <code>null</code>.
	 *
	 * @return The active context or <code>null</code>.
	 */
	protected Object getActiveContext(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId) {
		Assert.isNotNull(data);
		Assert.isNotNull(fullQualifiedId);
		Object activeContext = StepperAttributeUtil.getProperty(IStepAttributes.ATTR_ACTIVE_CONTEXT, fullQualifiedId, data);
		if (activeContext == null)
			activeContext = context.getContextObject();

		return activeContext;
	}
}
