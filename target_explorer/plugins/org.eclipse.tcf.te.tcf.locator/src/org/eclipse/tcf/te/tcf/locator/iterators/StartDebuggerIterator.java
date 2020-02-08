/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.locator.iterators;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.IStepAttributes;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Step group iterator for debugger attach.
 */
public class StartDebuggerIterator extends AbstractPeerNodeStepGroupIterator {

	/**
	 * Constructor.
	 */
	public StartDebuggerIterator() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.iterators.AbstractStepGroupIterator#initialize(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void initialize(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
	    super.initialize(context, data, fullQualifiedId, monitor);

	    final AtomicBoolean autoStartDbg = new AtomicBoolean(false);

	    final IPeerNode node = getActivePeerModelContext(context, data, fullQualifiedId);
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				String value = node.getPeer().getAttributes().get(IPeerProperties.PROP_AUTO_START_DEBUGGER);
				autoStartDbg.set(value != null ? Boolean.parseBoolean(value) : false);
			}
		};
		Protocol.invokeAndWait(runnable);

		setIterations(autoStartDbg.get() ? 1 : 0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.iterators.AbstractStepGroupIterator#internalNext(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void internalNext(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		StepperAttributeUtil.setProperty(IStepAttributes.ATTR_START_DEBUGGER, fullQualifiedId, data, true);
	}
}
