/**
 * StepperOperationService.java
 * Created on Apr 10, 2013
 *
 * Copyright (c) 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.locator.services;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IStepperServiceOperations;

/**
 * Connect/disconnect stepper operation service implementation.
 */
public class StepperOperationService extends org.eclipse.tcf.te.runtime.stepper.services.AbstractStepperOperationService {

	/**
	 * Constructor.
	 */
	public StepperOperationService() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#isHandledOperation(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isHandledOperation(Object context, String operation) {
		return IStepperServiceOperations.CONNECT.equals(operation) ||
						IStepperServiceOperations.DISCONNECT.equals(operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IStepperService#getStepGroupId(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getStepGroupId(Object context, String operation) {
		Assert.isTrue(context instanceof IPeerModel);

		if (IStepperServiceOperations.CONNECT.equals(operation)) {
			return "org.eclipse.tcf.te.tcf.locator.connectStepGroup"; //$NON-NLS-1$
		}
		if (IStepperServiceOperations.DISCONNECT.equals(operation)) {
			return "org.eclipse.tcf.te.tcf.locator.disconnectStepGroup"; //$NON-NLS-1$
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IStepperService#getStepGroupName(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getStepGroupName(Object context, String operation) {
		Assert.isTrue(context instanceof IPeerModel);

		if (IStepperServiceOperations.CONNECT.equals(operation)) {
			return "Connect "+((IPeerModel)context).getName(); //$NON-NLS-1$
		}
		if (IStepperServiceOperations.DISCONNECT.equals(operation)) {
			return "Disconnect "+((IPeerModel)context).getName(); //$NON-NLS-1$
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#isEnabled(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isEnabled(Object context, String operation) {
		if (context instanceof IConnectable) {
			if (IStepperServiceOperations.CONNECT.equals(operation)) {
				return ((IConnectable)context).isConnectStateChangeActionAllowed(IConnectable.ACTION_CONNECT);
			}
			if (IStepperServiceOperations.DISCONNECT.equals(operation)) {
				return ((IConnectable)context).isConnectStateChangeActionAllowed(IConnectable.ACTION_DISCONNECT);
			}
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#isCancelable(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isCancelable(Object context, String operation) {
		return IStepperServiceOperations.CONNECT.equals(operation);
	}
}
