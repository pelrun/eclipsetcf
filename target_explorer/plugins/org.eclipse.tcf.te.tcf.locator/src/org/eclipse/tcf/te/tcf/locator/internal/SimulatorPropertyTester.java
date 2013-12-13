/**
 * PropertyTester.java
 * Created on Mar 12, 2013
 *
 * Copyright (c) 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
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

			if (result.service != null) {
				if ("isSimulatorState".equals(property) && expectedValue instanceof String) { //$NON-NLS-1$
					State state = result.service.getState(receiver, result.settings);
					return state.toString().equalsIgnoreCase((String) expectedValue);
				}
				if ("canStartSimulator".equals(property) && expectedValue instanceof Boolean) { //$NON-NLS-1$
					State state = result.service.getState(receiver, result.settings);
					return state.equals(State.Stopped);
				}
			}
		}

		return false;
	}
}
