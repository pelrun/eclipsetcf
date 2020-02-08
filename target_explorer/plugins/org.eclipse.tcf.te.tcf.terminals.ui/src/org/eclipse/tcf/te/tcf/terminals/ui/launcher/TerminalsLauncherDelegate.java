/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.terminals.ui.launcher;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.terminals.core.interfaces.launcher.ITerminalsLauncher;
import org.eclipse.tcf.te.tcf.terminals.core.launcher.TerminalsLauncher;
import org.eclipse.tcf.te.tcf.terminals.ui.connector.TerminalsSettings;
import org.eclipse.tcf.te.tcf.terminals.ui.controls.TerminalsConfigurationPanel;
import org.eclipse.tm.internal.terminal.provisional.api.ISettingsStore;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.tm.internal.terminal.provisional.api.TerminalConnectorExtension;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tm.terminal.view.ui.interfaces.IConfigurationPanel;
import org.eclipse.tm.terminal.view.ui.interfaces.IConfigurationPanelContainer;
import org.eclipse.tm.terminal.view.ui.interfaces.IMementoHandler;
import org.eclipse.tm.terminal.view.ui.internal.SettingsStore;
import org.eclipse.tm.terminal.view.ui.launcher.AbstractLauncherDelegate;

/**
 * Terminals (TCF) launcher delegate implementation.
 */
@SuppressWarnings("restriction")
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
	public void execute(final Map<String, Object> properties, final ITerminalService.Done done) {
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

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.ILauncherDelegate#createTerminalConnector(java.util.Map)
	 */
    @Override
	public ITerminalConnector createTerminalConnector(Map<String, Object> properties) {
		Assert.isNotNull(properties);

    	// Check for the terminal connector id
    	String connectorId = (String)properties.get(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID);
		if (connectorId == null) connectorId = "org.eclipse.tcf.te.tcf.terminals.ui.TerminalsConnector"; //$NON-NLS-1$

		// Extract the streams properties
		OutputStream stdin = (OutputStream)properties.get(ITerminalsConnectorConstants.PROP_STREAMS_STDIN);
		InputStream stdout = (InputStream)properties.get(ITerminalsConnectorConstants.PROP_STREAMS_STDOUT);
		InputStream stderr = (InputStream)properties.get(ITerminalsConnectorConstants.PROP_STREAMS_STDERR);
		Object value = properties.get(ITerminalsConnectorConstants.PROP_LOCAL_ECHO);
		boolean localEcho = value instanceof Boolean ? ((Boolean)value).booleanValue() : false;
		String lineSeparator = (String)properties.get(ITerminalsConnectorConstants.PROP_LINE_SEPARATOR);
		ITerminalsLauncher launcher = (ITerminalsLauncher)properties.get(ITerminalsConnectorConstants.PROP_DATA);

		// Construct the terminal settings store
		ISettingsStore store = new SettingsStore();

		// Construct the terminals settings
		TerminalsSettings terminalsSettings = new TerminalsSettings();
		terminalsSettings.setStdinStream(stdin);
		terminalsSettings.setStdoutStream(stdout);
		terminalsSettings.setStderrStream(stderr);
		terminalsSettings.setLocalEcho(localEcho);
		terminalsSettings.setLineSeparator(lineSeparator);
		terminalsSettings.setTerminalsLauncher(launcher);
		// And save the settings to the store
		terminalsSettings.save(store);

		// Construct the terminal connector instance
		ITerminalConnector connector = TerminalConnectorExtension.makeTerminalConnector(connectorId);
		if (connector != null) {
			// Apply default settings
			connector.setDefaultSettings();
			// And load the real settings
			connector.load(store);
		}

		return connector;
	}
}
