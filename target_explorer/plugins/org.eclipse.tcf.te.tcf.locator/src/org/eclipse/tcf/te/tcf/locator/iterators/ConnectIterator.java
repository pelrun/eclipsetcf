/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.locator.iterators;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;

/**
 * Step group iterator for connect.
 */
public class ConnectIterator extends AbstractPeerNodeStepGroupIterator {

	/**
	 * Constructor.
	 */
	public ConnectIterator() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.iterators.AbstractStepGroupIterator#initialize(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void initialize(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
	    super.initialize(context, data, fullQualifiedId, monitor);

	    int connectState = getActivePeerModelContext(context, data, fullQualifiedId).getConnectState();

	    boolean doConnect = connectState == IConnectable.STATE_DISCONNECTED || connectState == IConnectable.STATE_CONNECT_SCHEDULED;
		setIterations(doConnect ? 1 : 0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.iterators.AbstractStepGroupIterator#internalNext(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void internalNext(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) throws CoreException {
	}
}
