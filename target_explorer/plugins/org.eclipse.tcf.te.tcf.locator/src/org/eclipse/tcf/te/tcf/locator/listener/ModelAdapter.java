/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.listener;

import org.eclipse.tcf.te.tcf.locator.interfaces.IModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Default model listener implementation.
 */
public class ModelAdapter implements IModelListener {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.IModelListener#locatorModelChanged(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel, org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, boolean)
	 */
	@Override
	public void locatorModelChanged(IPeerModel model, IPeerNode peerNode, boolean added) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.core.interfaces.IModelListener#locatorModelDisposed(org.eclipse.tcf.te.tcf.locator.core.interfaces.nodes.ILocatorModel)
	 */
	@Override
	public void locatorModelDisposed(IPeerModel model) {
	}

}
