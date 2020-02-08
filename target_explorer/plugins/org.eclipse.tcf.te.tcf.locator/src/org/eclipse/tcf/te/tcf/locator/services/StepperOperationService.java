/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.services;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.tcf.locator.interfaces.IStepGroupIds;
import org.eclipse.tcf.te.tcf.locator.interfaces.IStepperServiceOperations;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Connect/disconnect stepper operation service implementation.
 */
public class StepperOperationService extends org.eclipse.tcf.te.tcf.core.services.AbstractStepperOperationService {

	/**
	 * Constructor.
	 */
	public StepperOperationService() {
	}


	/**
	 * Returns the peer node context.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @return The peer node context.
	 */
	protected IPeerNode getPeerNodeContext(Object context) {
		IPeerNode peerNode = null;
		if (context instanceof IPeerNode)
			return (IPeerNode)context;
		if (context instanceof IAdaptable)
			peerNode = (IPeerNode)((IAdaptable)context).getAdapter(IPeerNode.class);
		if (peerNode == null)
			peerNode = (IPeerNode)Platform.getAdapterManager().getAdapter(context, IPeerNode.class);

		return peerNode;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#isHandledOperation(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isHandledOperation(Object context, String operation) {
		return super.isHandledOperation(context, operation) ||
						IStepperServiceOperations.CONNECT.equals(operation) ||
						IStepperServiceOperations.DISCONNECT.equals(operation) ||
						IStepperServiceOperations.CONNECTION_LOST.equals(operation) ||
						IStepperServiceOperations.CONNECTION_RECOVERING.equals(operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#addToActionHistory(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean addToActionHistory(Object context, String operation) {
	    return super.addToActionHistory(context, operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IStepperService#getStepGroupId(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getStepGroupId(Object context, String operation) {
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

		return super.getStepGroupId(context, operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.services.interfaces.IStepperService#getStepGroupName(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getStepGroupName(Object context, String operation) {
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

		return super.getStepGroupName(context, operation);
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

		return super.isEnabled(context, operation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService#isCancelable(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean isCancelable(Object context, String operation) {
		return super.isCancelable(context, operation) ||
						IStepperServiceOperations.CONNECT.equals(operation) ||
						IStepperServiceOperations.CONNECTION_RECOVERING.equals(operation);
	}
}
