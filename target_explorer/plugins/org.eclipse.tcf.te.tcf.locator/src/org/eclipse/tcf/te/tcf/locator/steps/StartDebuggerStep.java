/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.steps;

import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IRunControl;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IDebugService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModelProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelPeerNodeQueryService;

/**
 * Start debugger step implementation.
 */
public class StartDebuggerStep extends AbstractPeerModelStep {

	/**
	 * Constructor.
	 */
	public StartDebuggerStep() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
    public void validateExecute(final IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor) throws CoreException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
    public void execute(final IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, final ICallback callback) {
		final IPeerModel node = getActivePeerModelContext(context, data, fullQualifiedId);
		Assert.isNotNull(node);

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// Don't attach the debugger if no run control is provided by the target
				final ILocatorModelPeerNodeQueryService queryService = node.getModel().getService(ILocatorModelPeerNodeQueryService.class);
				Assert.isNotNull(queryService);
				queryService.queryServicesAsync(node, new ILocatorModelPeerNodeQueryService.DoneQueryServices() {
					@Override
					public void doneQueryServices(Throwable error) {
						if (error == null) {
							// Get the list of available remote services
							String remoteServices = node.getStringProperty(IPeerModelProperties.PROP_REMOTE_SERVICES);
							Assert.isNotNull(remoteServices);
							boolean canAttachDbg = false;
							StringTokenizer tokenizer = new StringTokenizer(remoteServices, ","); //$NON-NLS-1$
							while (tokenizer.hasMoreTokens()) {
								String svc = tokenizer.nextToken().trim();
								if (IRunControl.NAME.equals(svc)) {
									canAttachDbg = true;
									break;
								}
							}

							if (canAttachDbg) {
								Runnable runnable = new Runnable() {
									@Override
									public void run() {
										IDebugService dbgService = ServiceManager.getInstance().getService(node, IDebugService.class, false);
										if (dbgService != null) {
											// Attach the debugger and all cores (OCDDevices)
											IPropertiesContainer props = new PropertiesContainer();
											dbgService.attach(node, props, monitor, callback);
										}
										else {
											callback.done(StartDebuggerStep.this, Status.OK_STATUS);
										}
									}
								};
								Protocol.invokeLater(runnable);
							} else {
								callback.done(StartDebuggerStep.this, Status.OK_STATUS);
							}
						} else {
							callback.done(StartDebuggerStep.this, StatusHelper.getStatus(error));
						}
					}
				});
			}
		};

		Protocol.invokeLater(runnable);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.steps.AbstractStep#rollback(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.core.runtime.IStatus, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void rollback(IStepContext context, IPropertiesContainer data, IStatus status, IFullQualifiedId fullQualifiedId, IProgressMonitor monitor, ICallback callback) {
		final IPeerModel node = getActivePeerModelContext(context, data, fullQualifiedId);
		IDebugService dbgService = ServiceManager.getInstance().getService(node, IDebugService.class, false);
		if (dbgService != null) {
			IPropertiesContainer props = new PropertiesContainer();
			dbgService.detach(node, props, null, callback);
		}
		else {
			callback.done(this, Status.OK_STATUS);
		}
	}
}
