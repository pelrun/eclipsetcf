/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.wizards;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepContext;
import org.eclipse.tcf.te.runtime.stepper.interfaces.IStepperOperationService;
import org.eclipse.tcf.te.runtime.stepper.job.StepperJob;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.locator.interfaces.IStepperServiceOperations;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.ui.activator.UIPlugin;
import org.eclipse.tcf.te.ui.wizards.AbstractWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Abstract new configuration wizard implementation.
 */
public abstract class AbstractNewConfigWizard extends AbstractWizard implements INewWizard {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		super.init(workbench, selection);
		// Set the window title
		setWindowTitle(getWizardTitle());
		// Signal the need for a progress monitor
		setNeedsProgressMonitor(true);
	}

	/**
	 * Returns the new configuration wizard title.
	 *
	 * @return The wizard title. Never <code>null</code>.
	 */
	protected abstract String getWizardTitle();

	/**
	 * Returns if or if not the wizard should open the editor
	 * on "Finish". The default is <code>true</code>.
	 *
	 * @return <code>True</code> to open the editor, <code>false</code> otherwise.
	 */
	protected boolean isOpenEditorOnPerformFinish() {
		return true;
	}

	protected void postPerformFinish(final IPeerNode peerNode) {
		Assert.isNotNull(peerNode);

		// set new peer node as default context
		ServiceManager.getInstance().getService(IDefaultContextService.class).setDefaultContext(peerNode);

		// Determine if or if not to auto-connect the created connection.
		boolean autoConnect = true;
		// If set as system property, take the system property into account first
		if (System.getProperty("NoWizardAutoConnect") != null) { //$NON-NLS-1$
			autoConnect &= !Boolean.getBoolean("NoWizardAutoConnect"); //$NON-NLS-1$
		}
		// Apply the preference setting
		autoConnect &= !UIPlugin.getDefault().getPreferenceStore().getBoolean("NoWizardAutoConnect"); //$NON-NLS-1$

		// If auto-connect is switched off, we are done here.
		if (!autoConnect) return;

		// Connect and Attach the debugger
		final AtomicBoolean connect = new AtomicBoolean();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				connect.set(Boolean.parseBoolean(peerNode.getPeer().getAttributes().get(IPeerNodeProperties.PROP_AUTO_CONNECT)));
			}
		});

		if (connect.get()) {
			IService[] services = ServiceManager.getInstance().getServices(peerNode, IStepperOperationService.class, false);
			IStepperOperationService stepperOperationService = null;
			for (IService service : services) {
				if (service instanceof IStepperOperationService && ((IStepperOperationService)service).isHandledOperation(peerNode, IStepperServiceOperations.CONNECT)) {
					stepperOperationService = (IStepperOperationService)service;
					break;
				}
	        }
			if (stepperOperationService != null) {
				String stepGroupId = stepperOperationService.getStepGroupId(peerNode, IStepperServiceOperations.CONNECT);
				IStepContext stepContext = stepperOperationService.getStepContext(peerNode, IStepperServiceOperations.CONNECT);
				String name = stepperOperationService.getStepGroupName(peerNode, IStepperServiceOperations.CONNECT);
				IPropertiesContainer data = stepperOperationService.getStepData(peerNode, IStepperServiceOperations.CONNECT);
				boolean enabled = stepperOperationService.isEnabled(peerNode, IStepperServiceOperations.CONNECT);

				if (enabled && stepGroupId != null && stepContext != null) {
					try {
						StepperJob job = new StepperJob(name != null ? name : "", //$NON-NLS-1$
														stepContext,
														data,
														stepGroupId,
														IStepperServiceOperations.CONNECT,
														true,
														true);

						job.schedule();
					} catch (IllegalStateException e) {
						if (Platform.inDebugMode()) {
							UIPlugin.getDefault().getLog().log(StatusHelper.getStatus(e));
						}
					}
				}
			}
		}
	}
}
