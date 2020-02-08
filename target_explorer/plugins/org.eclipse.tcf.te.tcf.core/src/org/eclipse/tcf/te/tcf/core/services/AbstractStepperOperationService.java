/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.services;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.tcf.core.interfaces.IStepperServiceOperations;
import org.eclipse.tcf.te.tcf.core.nls.Messages;

/**
 * Channel manager stepper operation service implementation.
 */
public abstract class AbstractStepperOperationService extends org.eclipse.tcf.te.runtime.stepper.services.AbstractStepperOperationService {

	/**
	 * Constructor
	 */
	public AbstractStepperOperationService() {
		super();
	}

	/**
	 * Returns the peer context.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @return The peer context.
	 */
	protected IPeer getPeerContext(Object context) {
		IPeer peer = null;
		if (context instanceof IPeer)
			return (IPeer)context;
		if (context instanceof IAdaptable)
			peer = (IPeer)((IAdaptable)context).getAdapter(IPeer.class);
		if (peer == null)
			peer = (IPeer)Platform.getAdapterManager().getAdapter(context, IPeer.class);

		return peer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#isHandledOperation(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isHandledOperation(Object context, String operation) {
		return IStepperServiceOperations.OPEN_CHANNEL.equals(operation) || IStepperServiceOperations.CLOSE_CHANNEL.equals(operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#getStepGroupId(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getStepGroupId(Object context, String operation) {
		if (IStepperServiceOperations.OPEN_CHANNEL.equals(operation))
			return "org.eclipse.tcf.te.tcf.core.channelmanager.openChannelStepGroup"; //$NON-NLS-1$
		if (IStepperServiceOperations.CLOSE_CHANNEL.equals(operation))
			return "org.eclipse.tcf.te.tcf.core.channelmanager.closeChannelStepGroup"; //$NON-NLS-1$

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#getStepGroupName(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getStepGroupName(Object context, String operation) {
		if (IStepperServiceOperations.OPEN_CHANNEL.equals(operation))
			return NLS.bind(Messages.StepperOperationService_stepGroupName_openChannel, ((IPeer)context).getName());
		if (IStepperServiceOperations.CLOSE_CHANNEL.equals(operation))
			return NLS.bind(Messages.StepperOperationService_stepGroupName_closeChannel, ((IPeer)context).getName());

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#isEnabled(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isEnabled(Object context, String operation) {
		return IStepperServiceOperations.OPEN_CHANNEL.equals(operation) || IStepperServiceOperations.CLOSE_CHANNEL.equals(operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#isCancelable(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isCancelable(Object context, String operation) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#addToActionHistory(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean addToActionHistory(Object context, String operation) {
		return false;
	}

}
