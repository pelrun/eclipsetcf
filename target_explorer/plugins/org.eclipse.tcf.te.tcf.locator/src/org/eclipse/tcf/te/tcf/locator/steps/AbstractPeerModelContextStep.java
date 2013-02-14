/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.steps;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.runtime.stepper.extensions.AbstractStep;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;

/**
 * Abstract peer model context step
 */
public abstract class AbstractPeerModelContextStep extends AbstractStep {

	/**
	 * Determines the active peer model node from the given step context.
	 *
	 * @param context The step context. Must not be <code>null</code>.
	 * @return The peer model node or <code>null</code>.
	 */
	public IPeerModel getPeerModel(IStepContext context) {
		Assert.isNotNull(context);
		return (IPeerModel)context.getAdapter(IPeerModel.class);
	}
}
