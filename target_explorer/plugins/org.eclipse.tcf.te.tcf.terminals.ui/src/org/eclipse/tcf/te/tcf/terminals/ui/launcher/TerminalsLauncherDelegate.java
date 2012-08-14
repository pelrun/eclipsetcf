/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.terminals.ui.launcher;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.terminals.core.interfaces.launcher.ITerminalsLauncher;
import org.eclipse.tcf.te.tcf.terminals.core.launcher.TerminalsLauncher;
import org.eclipse.tcf.te.tcf.terminals.ui.controls.TerminalsWizardConfigurationPanel;
import org.eclipse.tcf.te.ui.controls.BaseDialogPageControl;
import org.eclipse.tcf.te.ui.terminals.interfaces.IConfigurationPanel;
import org.eclipse.tcf.te.ui.terminals.launcher.AbstractLauncherDelegate;

/**
 * Terminals (TCF) launcher delegate implementation.
 */
public class TerminalsLauncherDelegate extends AbstractLauncherDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate#getPanel(org.eclipse.tcf.te.ui.controls.BaseDialogPageControl)
	 */
	@Override
	public IConfigurationPanel getPanel(BaseDialogPageControl parentControl) {
	    return new TerminalsWizardConfigurationPanel(parentControl);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate#needsUserConfiguration()
	 */
	@Override
	public boolean needsUserConfiguration() {
	    return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate#execute(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	public void execute(IPropertiesContainer properties, ICallback callback) {
		Assert.isNotNull(properties);

		// Get the selection from the properties
		ISelection selection = (ISelection)properties.getProperty("selection"); //$NON-NLS-1$
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object element = ((IStructuredSelection)selection).getFirstElement();
			if (element instanceof IPeerModel) {
				final IPeerModel peerModel = (IPeerModel)element;
				final AtomicReference<IPeer> peer = new AtomicReference<IPeer>();
				if (Protocol.isDispatchThread()) {
					peer.set(peerModel.getPeer());
				} else {
					Protocol.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							peer.set(peerModel.getPeer());
						}
					});
				}

				if (peer.get() != null) {
					ITerminalsLauncher launcher = new TerminalsLauncher();
					launcher.launch(peer.get(), properties, callback);
				}
			}

		}
	}
}