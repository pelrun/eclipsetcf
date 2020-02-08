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
import org.eclipse.tcf.te.launch.core.persistence.filetransfer.FileTransfersPersistenceDelegate;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.interfaces.filetransfer.IFileTransferItem;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.steps.IFileSystemStepAttributes;

/**
 * Step group iterator for file transfer.
 */
public class FileTransferIterator extends AbstractTcfLaunchStepGroupIterator {

	private IFileTransferItem[] items = null;

	/**
	 * Constructor.
	 */
	public FileTransferIterator() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepGroupIterator#initialize(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void initialize(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		super.initialize(context, data, fullQualifiedId, monitor);
		items = FileTransfersPersistenceDelegate.getFileTransfers(getLaunchConfiguration(context));
		setIterations(items != null ? items.length : 0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.iterators.AbstractStepGroupIterator#internalNext(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void internalNext(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		StepperAttributeUtil.setProperty(IFileSystemStepAttributes.ATTR_FILE_TRANSFER_ITEM, fullQualifiedId, data, items[getIteration()]);
	}
}
