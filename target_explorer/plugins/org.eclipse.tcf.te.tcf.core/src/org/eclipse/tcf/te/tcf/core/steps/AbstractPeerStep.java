/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.steps;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.StepperAttributeUtil;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep;
import org.eclipse.tcf.te.tcf.core.interfaces.steps.ITcfStepAttributes;

/**
 * Abstract peer context step
 */
public abstract class AbstractPeerStep extends AbstractStep {

	/**
	 * Returns the active peer context that is currently used.
	 *
	 * @param context The step context. Must not be <code>null</code>.
	 * @param data The data giving object. Must not be <code>null</code>.
	 * @param fullQualifiedId The full qualified id for this step. Must not be <code>null</code>.
	 * @return The active peer context.
	 */
	protected IPeer getActivePeerContext(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId) {
		Object activeContext = getActiveContext(context, data, fullQualifiedId);
		IPeer peer = null;
		if (activeContext instanceof IPeer)
			return (IPeer)activeContext;
		if (activeContext instanceof IAdaptable)
			peer = (IPeer)((IAdaptable)activeContext).getAdapter(IPeer.class);
		if (peer == null)
			peer = (IPeer)Platform.getAdapterManager().getAdapter(activeContext, IPeer.class);

		return peer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep#cancel(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void cancel(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor) {
	    super.cancel(context, data, fullQualifiedId, monitor);
	    final IToken token = (IToken)StepperAttributeUtil.getProperty(ITcfStepAttributes.ATTR_RUNNING_TOKEN, fullQualifiedId, data);
    	StepperAttributeUtil.setProperty(ITcfStepAttributes.ATTR_RUNNING_TOKEN, fullQualifiedId, data, null);
	    if (token != null) {
	    	Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
			    	token.cancel();
				}
			});
	    }
	}
}
