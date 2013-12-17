/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.listener;

import org.eclipse.tcf.te.tcf.core.listeners.interfaces.IProtocolStateChangeListener;
import org.eclipse.tcf.te.tcf.locator.model.Model;

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
		if (state) Model.getPeerModel();
	}

}
