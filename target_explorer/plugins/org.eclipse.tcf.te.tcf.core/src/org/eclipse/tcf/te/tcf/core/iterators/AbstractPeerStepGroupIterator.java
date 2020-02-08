/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.core.iterators;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.iterators.AbstractStepGroupIterator;

/**
 * Abstract TCF launch step group iterator.
 */
public abstract class AbstractPeerStepGroupIterator extends AbstractStepGroupIterator {

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
}
