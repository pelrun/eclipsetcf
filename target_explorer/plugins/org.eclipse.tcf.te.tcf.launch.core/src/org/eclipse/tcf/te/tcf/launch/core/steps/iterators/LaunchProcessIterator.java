/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.launch.core.steps.iterators;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.tcf.te.launch.core.persistence.DefaultPersistenceDelegate;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.steps.IProcessesStepAttributes;

/**
 * Step group iterator for file transfer.
 */
public class LaunchProcessIterator extends AbstractTcfLaunchStepGroupIterator {

	/**
	 * Constructor.
	 */
	public LaunchProcessIterator() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.iterators.AbstractStepGroupIterator#initialize(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void initialize(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
	    super.initialize(context, data, fullQualifiedId, monitor);
	    setIterations(1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.iterators.AbstractStepGroupIterator#internalNext(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void internalNext(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		String processImage = DefaultPersistenceDelegate.getAttribute(getLaunchConfiguration(context), IProcessesStepAttributes.ATTR_PROCESS_IMAGE, (String)null);
		String processArguments = DefaultPersistenceDelegate.getAttribute(getLaunchConfiguration(context), IProcessesStepAttributes.ATTR_PROCESS_ARGUMENTS, (String)null);

		boolean isDebug = ILaunchManager.DEBUG_MODE.equals(getLaunchMode(context));
		boolean attachProcess = isDebug;
		boolean stopAtEntry = isDebug && DefaultPersistenceDelegate.getAttribute(getLaunchConfiguration(context), IProcessesStepAttributes.ATTR_STOP_AT_ENTRY, false);
		boolean stopAtMain = isDebug && DefaultPersistenceDelegate.getAttribute(getLaunchConfiguration(context), IProcessesStepAttributes.ATTR_STOP_AT_MAIN, false);
		boolean attachChildren = isDebug && DefaultPersistenceDelegate.getAttribute(getLaunchConfiguration(context), IProcessesStepAttributes.ATTR_ATTACH_CHILDREN, false);

		boolean outputConsole = DefaultPersistenceDelegate.getAttribute(getLaunchConfiguration(context), "org.eclipse.debug.ui.ATTR_CONSOLE_OUTPUT_ON", true); //$NON-NLS-1$
		String outputFile = DefaultPersistenceDelegate.getAttribute(getLaunchConfiguration(context), "org.eclipse.debug.ui.ATTR_CAPTURE_IN_FILE", (String)null); //$NON-NLS-1$

		StepperAttributeUtil.setProperty(IProcessesStepAttributes.ATTR_PROCESS_IMAGE, fullQualifiedId, data, processImage);
		StepperAttributeUtil.setProperty(IProcessesStepAttributes.ATTR_PROCESS_ARGUMENTS, fullQualifiedId, data, processArguments);
		StepperAttributeUtil.setProperty(IProcessesStepAttributes.ATTR_ATTACH, fullQualifiedId, data, attachProcess);
		StepperAttributeUtil.setProperty(IProcessesStepAttributes.ATTR_ATTACH_CHILDREN, fullQualifiedId, data, attachChildren);
		StepperAttributeUtil.setProperty(IProcessesStepAttributes.ATTR_STOP_AT_ENTRY, fullQualifiedId, data, stopAtEntry);
		StepperAttributeUtil.setProperty(IProcessesStepAttributes.ATTR_STOP_AT_MAIN, fullQualifiedId, data, stopAtMain);
		StepperAttributeUtil.setProperty(IProcessesStepAttributes.ATTR_OUTPUT_CONSOLE, fullQualifiedId, data, outputConsole);
		StepperAttributeUtil.setProperty(IProcessesStepAttributes.ATTR_OUTPUT_FILE, fullQualifiedId, data, outputFile);
	}
}
