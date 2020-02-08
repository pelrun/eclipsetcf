/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.statushandler;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.statushandler.AbstractStatusHandler;

/**
 * Test status handler implementation.
 */
public class TestStatusHandler extends AbstractStatusHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.statushandler.interfaces.IStatusHandler#handleStatus(org.eclipse.core.runtime.IStatus, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void handleStatus(IStatus status, IPropertiesContainer data, ICallback done) {
	}
}
