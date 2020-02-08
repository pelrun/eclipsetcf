/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.steps;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.interfaces.filetransfer.IFileTransferItem;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;
import org.eclipse.tcf.te.tcf.core.steps.AbstractPeerStep;
import org.eclipse.tcf.te.tcf.filesystem.core.activator.CorePlugin;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.steps.IFileSystemStepAttributes;
import org.eclipse.tcf.te.tcf.filesystem.core.services.FileTransferService;

/**
 * Launch process step implementation.
 */
public class FileTransferStep extends AbstractPeerStep {

	/**
	 * Constructor.
	 */
	public FileTransferStep() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IExtendedStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
		IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);
		if (channel == null || channel.getState() != IChannel.STATE_OPEN) {
			throw new CoreException(new Status(IStatus.ERROR, CorePlugin.getUniqueIdentifier(), "missing or closed channel")); //$NON-NLS-1$
		}

		Object item = StepperAttributeUtil.getProperty(IFileSystemStepAttributes.ATTR_FILE_TRANSFER_ITEM, fullQualifiedId, data);
		if (!(item instanceof IFileTransferItem)) {
			throw new CoreException(new Status(IStatus.ERROR, CorePlugin.getUniqueIdentifier(), "missing file transfer item")); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor, final ICallback callback) {
		final IChannel channel = (IChannel)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_CHANNEL, fullQualifiedId, data);
		final IFileTransferItem item = (IFileTransferItem)StepperAttributeUtil.getProperty(IFileSystemStepAttributes.ATTR_FILE_TRANSFER_ITEM, fullQualifiedId, data);

		if (item.isEnabled()) {
			FileTransferService.transfer(getActivePeerContext(context, data, fullQualifiedId), channel, item, monitor, callback);
		}
		else {
			callback.done(this, Status.OK_STATUS);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.extensions.AbstractStep#getTotalWork(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public int getTotalWork(IStepContext context, IPropertiesContainer data) {
		return 1000;
	}
}
