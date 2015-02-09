/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.terminals.ui.launcher;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.terminals.interfaces.ITerminalService.Done;
import org.eclipse.tcf.te.core.terminals.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.terminals.core.interfaces.launcher.ITerminalsLauncher;
import org.eclipse.tcf.te.tcf.terminals.core.launcher.TerminalsLauncher;
import org.eclipse.tcf.te.tcf.terminals.ui.controls.TerminalsConfigurationPanel;
import org.eclipse.tcf.te.ui.terminals.interfaces.IConfigurationPanel;
import org.eclipse.tcf.te.ui.terminals.interfaces.IConfigurationPanelContainer;
import org.eclipse.tcf.te.ui.terminals.interfaces.IMementoHandler;
import org.eclipse.tcf.te.ui.terminals.launcher.AbstractLauncherDelegate;

/**
 * Terminals (TCF) launcher delegate implementation.
 */
public class TerminalsLauncherDelegate extends AbstractLauncherDelegate {
	// The Terminals (TCF) terminal connection memento handler
	private final IMementoHandler mementoHandler = new TerminalsMementoHandler();

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate#getPanel(org.eclipse.tcf.te.ui.terminals.interfaces.IConfigurationPanelContainer)
	 */
	@Override
	public IConfigurationPanel getPanel(IConfigurationPanelContainer container) {
	    return new TerminalsConfigurationPanel(container);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate#needsUserConfiguration()
	 */
	@Override
	public boolean needsUserConfiguration() {
	    return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate#execute(java.util.Map, org.eclipse.tcf.te.core.terminals.interfaces.ITerminalService.Done)
	 */
	@Override
	public void execute(final Map<String, Object> properties, final Done done) {
		Assert.isNotNull(properties);

		// Get the selection from the properties
		ISelection selection = (ISelection)properties.get(ITerminalsConnectorConstants.PROP_SELECTION);
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
					IPropertiesContainer p = new PropertiesContainer();
					p.addProperties(properties);
					launcher.launch(peer.get(), p, new Callback() {
						@Override
						protected void internalDone(Object caller, IStatus status) {
							done.done(status);
						}
					});
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
