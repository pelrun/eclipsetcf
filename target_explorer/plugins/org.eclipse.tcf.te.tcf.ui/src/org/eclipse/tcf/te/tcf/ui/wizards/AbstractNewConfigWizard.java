/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.wizards;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
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
				connect.set(Boolean.parseBoolean(peerNode.getPeer().getAttributes().get(IPeerProperties.PROP_AUTO_CONNECT)));
			}
		});

		IPathMapService service = ServiceManager.getInstance().getService(peerNode, IPathMapService.class);
		if (service != null) {
			service.generateSourcePathMappings(peerNode);
		}

		if (connect.get() && peerNode.isValid()) {
			peerNode.changeConnectState(IConnectable.ACTION_CONNECT, null, null);
		}
	}
}
