/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.steps;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IDebugService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;

/**
 * Stop debugger step implementation.
 */
public class StopDebuggerStep extends AbstractPeerNodeStep {

	/**
	 * Constructor.
	 */
	public StopDebuggerStep() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IExtendedStep#validateExecute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void validateExecute(final IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor) throws CoreException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.stepper.interfaces.IStep#execute(org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.stepper.interfaces.IFullQualifiedId, org.eclipse.core.runtime.IProgressMonitor, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(final IStepContext context, final IPropertiesContainer data, final IFullQualifiedId fullQualifiedId, final IProgressMonitor monitor, final ICallback callback) {
		IDebugService dbgService = ServiceManager.getInstance().getService(getActivePeerModelContext(context, data, fullQualifiedId), IDebugService.class, false);
		if (dbgService != null) {
			final IPropertiesContainer props = new PropertiesContainer();
			dbgService.detach(getActivePeerModelContext(context, data, fullQualifiedId), props, monitor, new Callback() {
				/* (non-Javadoc)
				 * @see org.eclipse.tcf.te.runtime.callback.Callback#internalDone(java.lang.Object, org.eclipse.core.runtime.IStatus)
				 */
				@Override
				protected void internalDone(Object caller, IStatus status) {
					Protocol.invokeLater(new Runnable() {
						@Override
						public void run() {
							getActivePeerModelContext(context, data, fullQualifiedId).setProperty(IDebugService.PROPERTY_DEBUGGER_DETACHED, props.getBooleanProperty(IDebugService.PROPERTY_DEBUGGER_DETACHED));
						}
					});
					callback.done(this, Status.OK_STATUS);
				}
			});
		}
		else {
			callback.done(this, Status.OK_STATUS);
		}
	}
}
