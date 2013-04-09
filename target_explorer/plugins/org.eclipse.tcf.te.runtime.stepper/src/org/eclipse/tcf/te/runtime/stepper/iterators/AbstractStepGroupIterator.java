/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.runtime.stepper.iterators;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.te.runtime.extensions.ExecutableExtension;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepAttributes;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupIterator;

/**
 * Abstract step group iterator.
 */
public abstract class AbstractStepGroupIterator extends ExecutableExtension implements IStepGroupIterator {

	private int iteration = -1;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupIterator#initialize(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void initialize(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) {
		iteration = 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupIterator#hasNext(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public final boolean hasNext(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) {
		return iteration < getNumIterations();
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
	public void next(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		iteration++;
	}

	/**
	 * Set the active context.
	 *
	 * @param activeContext The new active context.
	 * @param data The data giving object. Must not be <code>null</code>.
	 * @param fullQualifiedId The full qualfied id for this step. Must not be <code>null</code>.
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
	 * @param fullQualifiedId The full qualfied id for this step. Must not be <code>null</code>.
	 *
	 * @return The active context or <code>null</code>.
	 */
	protected Object getActiveContext(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId) {
		Assert.isNotNull(data);
		Assert.isNotNull(fullQualifiedId);
		return StepperAttributeUtil.getProperty(IStepAttributes.ATTR_ACTIVE_CONTEXT, fullQualifiedId, data);
	}
}
