/**
 * AbstractSimulatorService.java
 * Created on Mar 22, 2013
 *
 * Copyright (c) 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.locator.services;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IDebugService;
import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModelProperties;

/**
 * Abstract simulator service implementation.
 */
public abstract class AbstractSimulatorService extends AbstractService implements ISimulatorService {

	public static final String CALLBACK_SIMULATOR_EXIT_VALUE = "simulatorExitValue"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see com.windriver.te.tcf.core.interfaces.agents.IAgentService#autoStartDebugger(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void autoStartDebugger(final IPeerModel peerModel, final IProgressMonitor monitor) {
		Assert.isNotNull(peerModel);

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				String value = peerModel.getPeer().getAttributes().get(IPeerModelProperties.PROP_AUTO_START_DEBUGGER);
				boolean autoStartDbg = value != null ? Boolean.parseBoolean(value) : false;

				// Auto-start the debugger now, if requested
				if (autoStartDbg) {
					// If the peer model is in state WAITING_FOR_READY, the debugger
					// launch needs to be delayed until the state is reseted.
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							IDebugService dbgService = ServiceManager.getInstance().getService(peerModel, IDebugService.class, false);
							if (dbgService != null) {
								// Attach the debugger and all cores (OCDDevices)
								IPropertiesContainer props = new PropertiesContainer();
								dbgService.attach(peerModel, props, new Callback());
							}
						}
					};

					Protocol.invokeLater(runnable);
				}
			}
		};

		Protocol.invokeLater(runnable);
	}
}
