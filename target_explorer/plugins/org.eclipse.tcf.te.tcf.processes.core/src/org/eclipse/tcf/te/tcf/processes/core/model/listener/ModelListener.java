/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.listener;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.listener.ModelAdapter;
import org.eclipse.tcf.te.tcf.processes.core.model.ModelManager;

/**
 * Locator model listener implementation.
 */
public class ModelListener extends ModelAdapter {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.listener.ModelAdapter#modelChanged(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel, org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, boolean)
	 */
	@Override
	public void modelChanged(final IPeerModel model, final IPeerNode peerNode, final boolean added) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		if (peerNode == null) return;

		if (!added) {
			// Dispose possibly associated models
			ModelManager.disposeRuntimeModel(peerNode);
		}
	}
}
