/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.internal.channelmanager;

import org.eclipse.core.runtime.Assert;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.runtime.stepper.services.AbstractStepperOperationService;
import org.eclipse.tcf.te.tcf.core.nls.Messages;

/**
 * Channel manager stepper operation service implementation.
 */
public class StepperOperationService extends AbstractStepperOperationService {
	/**
	 * Open channel operation
	 */
	public static final String OPEN_CHANNEL = "openChannel"; //$NON-NLS-1$

	/**
	 * Close channel operation
	 */
	public static final String CLOSE_CHANNEL = "closeChannel"; //$NON-NLS-1$

	/**
	 * Constructor
	 */
	public StepperOperationService() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#isHandledOperation(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isHandledOperation(Object context, String operation) {
		Assert.isTrue(context instanceof IPeer);
		return OPEN_CHANNEL.equals(operation) || CLOSE_CHANNEL.equals(operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#getStepGroupId(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getStepGroupId(Object context, String operation) {
		Assert.isTrue(context instanceof IPeer);

		if (OPEN_CHANNEL.equals(operation))
			return "org.eclipse.tcf.te.tcf.core.channelmanager.openChannelStepGroup"; //$NON-NLS-1$
		if (CLOSE_CHANNEL.equals(operation))
			return "org.eclipse.tcf.te.tcf.core.channelmanager.closeChannelStepGroup"; //$NON-NLS-1$

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#getStepGroupName(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getStepGroupName(Object context, String operation) {
		Assert.isTrue(context instanceof IPeer);

		if (OPEN_CHANNEL.equals(operation))
			return NLS.bind(Messages.StepperOperationService_stepGroupName_openChannel, ((IPeer)context).getName());
		if (CLOSE_CHANNEL.equals(operation))
			return NLS.bind(Messages.StepperOperationService_stepGroupName_closeChannel, ((IPeer)context).getName());

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#isEnabled(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isEnabled(Object context, String operation) {
		Assert.isTrue(context instanceof IPeer);
		return OPEN_CHANNEL.equals(operation) || CLOSE_CHANNEL.equals(operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#isCancelable(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isCancelable(Object context, String operation) {
		Assert.isTrue(context instanceof IPeer);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#addToActionHistory(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean addToActionHistory(Object context, String operation) {
		Assert.isTrue(context instanceof IPeer);
		return false;
	}

}
