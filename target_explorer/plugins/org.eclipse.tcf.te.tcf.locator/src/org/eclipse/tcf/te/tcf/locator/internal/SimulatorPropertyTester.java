/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.internal;

import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService.State;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.utils.SimulatorUtils;
import org.eclipse.tcf.te.tcf.locator.utils.SimulatorUtils.Result;

/**
 * Property tester implementation.
 */
public class SimulatorPropertyTester extends org.eclipse.core.expressions.PropertyTester {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String,
	 * java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(final Object receiver, String property, Object[] args, Object expectedValue) {
		if (receiver instanceof IPeerNode) {
			final IPeerNode peerNode = (IPeerNode) receiver;
			Result result = SimulatorUtils.getSimulatorService(peerNode);

			if (result != null && result.service != null) {
				if ("isSimulatorState".equals(property) && expectedValue instanceof String) { //$NON-NLS-1$
					State state = result.service.getState(receiver, result.settings);
					return state.toString().equalsIgnoreCase((String) expectedValue);
				}
				if ("canStartSimulator".equals(property) && expectedValue instanceof Boolean) { //$NON-NLS-1$
					State state = result.service.getState(receiver, result.settings);
					return state.equals(State.Stopped);
				}
				if ("isSimulator".equals(property)) { //$NON-NLS-1$
					if (expectedValue instanceof String) {
						return ((String)expectedValue).equals(result.id);
					}
					return true;
				}
			}
		}

		return false;
	}
}
