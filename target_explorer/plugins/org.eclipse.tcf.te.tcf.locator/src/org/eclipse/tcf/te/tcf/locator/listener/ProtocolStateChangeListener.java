/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.listener;

import org.eclipse.tcf.te.tcf.core.listeners.interfaces.IProtocolStateChangeListener;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;

/**
 * Protocol state change listener implementation.
 */
public class ProtocolStateChangeListener implements IProtocolStateChangeListener {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.core.listeners.interfaces.IProtocolStateChangeListener#stateChanged(boolean)
	 */
	@Override
	public void stateChanged(boolean state) {
		// If the TCF frame work got started, initialize the locator model as well
		if (state) ModelManager.getPeerModel();
	}

}
