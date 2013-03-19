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

import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService;
import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService.State;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModelProperties;

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
		if (receiver instanceof IPeerModel) {
			final IPeerModel peerModel = (IPeerModel) receiver;
			final IPropertiesContainer simSetting = new PropertiesContainer();
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					simSetting.setProperty(IPeerModelProperties.PROP_SIM_ENABLED, peerModel.getPeer().getAttributes().get(IPeerModelProperties.PROP_SIM_ENABLED));
					simSetting.setProperty(IPeerModelProperties.PROP_SIM_TYPE, peerModel.getPeer().getAttributes().get(IPeerModelProperties.PROP_SIM_TYPE));
					simSetting.setProperty(IPeerModelProperties.PROP_SIM_PROPERTIES, peerModel.getPeer().getAttributes().get(IPeerModelProperties.PROP_SIM_PROPERTIES));
				}
			};

			if (Protocol.isDispatchThread()) {
				runnable.run();
			}
			else {
				Protocol.invokeAndWait(runnable);
			}

			ISimulatorService simService = null;
			for (IService service : ServiceManager.getInstance().getServices(receiver, ISimulatorService.class, false)) {
				if (service instanceof ISimulatorService &&
								service.getId().equals(simSetting.getStringProperty(IPeerModelProperties.PROP_SIM_TYPE))) {
					simService = (ISimulatorService) service;
					break;
				}
			}

			if (simService != null) {
				if ("isSimulatorState".equals(property) && expectedValue instanceof String) { //$NON-NLS-1$
					State state = simService.getState(receiver, simSetting.getStringProperty(IPeerModelProperties.PROP_SIM_PROPERTIES));
					return state.toString().equalsIgnoreCase((String) expectedValue);
				}
				if ("canStartSimulator".equals(property) && expectedValue instanceof Boolean) { //$NON-NLS-1$
					State state = simService.getState(receiver, simSetting.getStringProperty(IPeerModelProperties.PROP_SIM_PROPERTIES));
					return state.equals(State.Stopped) &&
									simSetting.getBooleanProperty(IPeerModelProperties.PROP_SIM_ENABLED) == ((Boolean) expectedValue).booleanValue();
				}
			}
		}

		return false;
	}
}
