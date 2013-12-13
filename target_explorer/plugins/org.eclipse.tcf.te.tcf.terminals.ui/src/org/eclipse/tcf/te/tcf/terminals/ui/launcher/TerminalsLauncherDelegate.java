/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
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
import org.eclipse.tcf.te.runtime.services.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.terminals.core.interfaces.launcher.ITerminalsLauncher;
import org.eclipse.tcf.te.tcf.terminals.core.launcher.TerminalsLauncher;
import org.eclipse.tcf.te.tcf.terminals.ui.controls.TerminalsWizardConfigurationPanel;
import org.eclipse.tcf.te.ui.controls.BaseDialogPageControl;
import org.eclipse.tcf.te.ui.terminals.interfaces.IConfigurationPanel;
import org.eclipse.tcf.te.ui.terminals.interfaces.IMementoHandler;
import org.eclipse.tcf.te.ui.terminals.launcher.AbstractLauncherDelegate;

/**
 * Terminals (TCF) launcher delegate implementation.
 */
public class TerminalsLauncherDelegate extends AbstractLauncherDelegate {
	// The Terminals (TCF) terminal connection memento handler
	private final IMementoHandler mementoHandler = new TerminalsMementoHandler();

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
		ISelection selection = (ISelection)properties.getProperty(ITerminalsConnectorConstants.PROP_SELECTION);
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object element = ((IStructuredSelection)selection).getFirstElement();
			if (element instanceof IPeerNode) {
				final IPeerNode peerNode = (IPeerNode)element;
				final AtomicReference<IPeer> peer = new AtomicReference<IPeer>();
				if (Protocol.isDispatchThread()) {
					peer.set(peerNode.getPeer());
				} else {
					Protocol.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							peer.set(peerNode.getPeer());
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

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (IMementoHandler.class.equals(adapter)) {
			return mementoHandler;
		}
	    return super.getAdapter(adapter);
	}
}
