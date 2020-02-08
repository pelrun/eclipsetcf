/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.steps;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.tcf.core.steps.AbstractPeerStep;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Abstract peer model context step
 */
public abstract class AbstractPeerNodeStep extends AbstractPeerStep {

	/**
	 * Returns the active peer model context that is currently used.
	 *
	 * @param context The step context. Must not be <code>null</code>.
	 * @param data The data giving object. Must not be <code>null</code>.
	 * @param fullQualifiedId The full qualified id for this step. Must not be <code>null</code>.
	 * @return The active peer model context.
	 */
	protected IPeerNode getActivePeerModelContext(IStepContext context, IPropertiesContainer data, IFullQualifiedId fullQualifiedId) {
		Object activeContext = getActiveContext(context, data, fullQualifiedId);
		IPeerNode peerNode = null;
		if (activeContext instanceof IPeerNode)
			return (IPeerNode)activeContext;
		if (activeContext instanceof IAdaptable)
			peerNode = (IPeerNode)((IAdaptable)activeContext).getAdapter(IPeerNode.class);
		if (peerNode == null)
			peerNode = (IPeerNode)Platform.getAdapterManager().getAdapter(activeContext, IPeerNode.class);

		return peerNode;
	}
}
