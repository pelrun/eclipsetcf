/**
 * StepperOperationService.java
 * Created on Apr 10, 2013
 *
 * Copyright (c) 2013, 2014 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.locator.services;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.tcf.locator.interfaces.IStepGroupIds;
import org.eclipse.tcf.te.tcf.locator.interfaces.IStepperServiceOperations;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

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
						IStepperServiceOperations.DISCONNECT.equals(operation) ||
						IStepperServiceOperations.CONNECTION_LOST.equals(operation) ||
						IStepperServiceOperations.CONNECTION_RECOVERING.equals(operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#addToActionHistory(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean addToActionHistory(Object context, String operation) {
	    return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IStepperService#getStepGroupId(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getStepGroupId(Object context, String operation) {
		Assert.isTrue(context instanceof IPeerNode);

		if (IStepperServiceOperations.CONNECT.equals(operation)) {
			return IStepGroupIds.CONNECT;
		}
		if (IStepperServiceOperations.DISCONNECT.equals(operation)) {
			return IStepGroupIds.DISCONNECT;
		}
		if (IStepperServiceOperations.CONNECTION_LOST.equals(operation)) {
			return IStepGroupIds.CONNECTON_LOST;
		}
		if (IStepperServiceOperations.CONNECTION_RECOVERING.equals(operation)) {
			return IStepGroupIds.CONNECTION_RECOVERING;
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IStepperService#getStepGroupName(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getStepGroupName(Object context, String operation) {
		Assert.isTrue(context instanceof IPeerNode);

		if (IStepperServiceOperations.CONNECT.equals(operation)) {
			return "Connect "+((IPeerNode)context).getName(); //$NON-NLS-1$
		}
		if (IStepperServiceOperations.DISCONNECT.equals(operation)) {
			return "Disconnect "+((IPeerNode)context).getName(); //$NON-NLS-1$
		}
		if (IStepperServiceOperations.CONNECTION_LOST.equals(operation)) {
			return "Lost Connection to "+((IPeerNode)context).getName(); //$NON-NLS-1$
		}
		if (IStepperServiceOperations.CONNECTION_RECOVERING.equals(operation)) {
			return "Recovering Connection to "+((IPeerNode)context).getName(); //$NON-NLS-1$
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
			if (IStepperServiceOperations.CONNECTION_LOST.equals(operation)) {
				return ((IConnectable)context).isConnectStateChangeActionAllowed(IConnectable.STATE_CONNECTION_LOST);
			}
			if (IStepperServiceOperations.CONNECTION_RECOVERING.equals(operation)) {
				return ((IConnectable)context).isConnectStateChangeActionAllowed(IConnectable.STATE_CONNECTION_RECOVERING);
			}
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#isCancelable(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isCancelable(Object context, String operation) {
		return IStepperServiceOperations.CONNECT.equals(operation) || IStepperServiceOperations.CONNECTION_RECOVERING.equals(operation);
	}
}
