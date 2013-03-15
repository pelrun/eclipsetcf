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

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService;
import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService.State;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModelProperties;

/**
 * Property tester implementation.
 */
public class SimulatorPropertyTester extends org.eclipse.core.expressions.PropertyTester {

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(final Object receiver, String property, Object[] args, Object expectedValue) {
		if (receiver instanceof IPeerModel) {

			if ("isSimulatorState".equals(property) && expectedValue instanceof String) { //$NON-NLS-1$
				// Get the simulator service
				ISimulatorService service = ServiceManager.getInstance().getService(receiver, ISimulatorService.class);
				if (service != null) {
					State state = service.getState(receiver, null);

					return state.toString().equalsIgnoreCase((String)expectedValue);
				}
			}
			if ("canStartSimulator".equals(property) && expectedValue instanceof Boolean) { //$NON-NLS-1$
				// Get the simulator service
				ISimulatorService service = ServiceManager.getInstance().getService(receiver, ISimulatorService.class);
				if (service != null) {
					State state = service.getState(receiver, null);
					final AtomicBoolean simEnabled = new AtomicBoolean(false);

					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							simEnabled.set(Boolean.valueOf(((IPeerModel)receiver).getPeer().getAttributes().get(IPeerModelProperties.PROP_SIM_ENABLED)).booleanValue());
						}
					};
					if (Protocol.isDispatchThread())
						runnable.run();
					else
						Protocol.invokeAndWait(runnable);

					return (state.equals(State.Stopped) && simEnabled.get()) == ((Boolean)expectedValue).booleanValue();
				}
			}
		}

		return false;
	}
}
