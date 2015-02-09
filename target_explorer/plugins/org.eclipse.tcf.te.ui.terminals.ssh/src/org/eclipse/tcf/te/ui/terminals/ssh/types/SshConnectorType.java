/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Max Weninger (Wind River) - [361352] [TERMINALS][SSH] Add SSH terminal support
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.ssh.types;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.core.terminals.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tcf.te.ui.terminals.internal.SettingsStore;
import org.eclipse.tcf.te.ui.terminals.types.AbstractConnectorType;
import org.eclipse.tm.internal.terminal.provisional.api.ISettingsStore;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.tm.internal.terminal.provisional.api.TerminalConnectorExtension;
import org.eclipse.tm.internal.terminal.ssh.SshSettings;

/**
 * Ssh terminal connector type implementation.
 */
@SuppressWarnings("restriction")
public class SshConnectorType extends AbstractConnectorType {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.IConnectorType#createTerminalConnector(java.util.Map)
	 */
	@Override
	public ITerminalConnector createTerminalConnector(Map<String, Object> properties) {
    	Assert.isNotNull(properties);

    	// Check for the terminal connector id
    	String connectorId = (String)properties.get(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID);
		if (connectorId == null) connectorId = "org.eclipse.tm.internal.terminal.ssh.SshConnector"; //$NON-NLS-1$

		// Extract the ssh properties
		String host = (String)properties.get(ITerminalsConnectorConstants.PROP_IP_HOST);
		Object value = properties.get(ITerminalsConnectorConstants.PROP_IP_PORT);
		String port = value != null ? value.toString() : null;
		value = properties.get(ITerminalsConnectorConstants.PROP_TIMEOUT);
		String timeout = value != null ? value.toString() : null;
		value = properties.get(ITerminalsConnectorConstants.PROP_SSH_KEEP_ALIVE);
		String keepAlive = value != null ? value.toString() : null;
		String password = (String)properties.get(ITerminalsConnectorConstants.PROP_SSH_PASSWORD);
		String user = (String)properties.get(ITerminalsConnectorConstants.PROP_SSH_USER);

		int portOffset = 0;
		if (properties.get(ITerminalsConnectorConstants.PROP_IP_PORT_OFFSET) instanceof Integer) {
			portOffset = ((Integer)properties.get(ITerminalsConnectorConstants.PROP_IP_PORT_OFFSET)).intValue();
			if (portOffset < 0) portOffset = 0;
		}

		return host != null && port != null ? createSshConnector(connectorId, new String[] { host, port, timeout, keepAlive, password, user }, portOffset) : null;
	}

	/**
	 * Creates a ssh connector object based on the given ssh server attributes.
	 * <p>
	 * The ssh server attributes must contain at least 2 elements:
	 * <ul>
	 * <li>attributes[0] --> ssh server host name</li>
	 * <li>attributes[1] --> ssh port</li>
	 * <li>attributes[2] --> timeout</li>
	 * <li>attributes[3] --> keep alive</li>
	 * <li>attributes[4] --> ssh password</li>
	 * <li>attributes[5] --> ssh user</li>
	 * </ul>
	 *
	 * @param connectorId The terminal connector id. Must not be <code>null</code>.
	 * @param attributes The ssh server attributes. Must not be <code>null</code>.
	 * @param portOffset Offset to add to the port.
	 *
	 * @return The terminal connector object instance or <code>null</code>.
	 */
	protected ITerminalConnector createSshConnector(String connectorId, String[] attributes, int portOffset) {
		Assert.isNotNull(connectorId);
		Assert.isNotNull(attributes);
		Assert.isTrue(attributes.length == 6);

		final String serverName = attributes[0];
		final String serverPort = Integer.toString(Integer.decode(attributes[1]).intValue() + portOffset);
		final String timeout = attributes[2];
		final String keepAlive=attributes[3];
		final String password=attributes[4];
		final String user=attributes[5];

		// Construct the ssh settings store
		ISettingsStore store = new SettingsStore();

		// Construct the telnet settings
		SshSettings sshSettings = new SshSettings();
		sshSettings.setHost(serverName);
		sshSettings.setPort(serverPort);
		sshSettings.setTimeout(timeout);
		sshSettings.setKeepalive(keepAlive);
		sshSettings.setPassword(password);
		sshSettings.setUser(user);

		// And save the settings to the store
		sshSettings.save(store);

		// MWE TODO make sure this is NOT passed outside as this is plain text
		store.put("Password", password); //$NON-NLS-1$

		// Construct the terminal connector instance
		ITerminalConnector connector = TerminalConnectorExtension.makeTerminalConnector(connectorId);
		if (connector != null) {
			// Apply default settings
			connector.makeSettingsPage();
			// And load the real settings
			connector.load(store);
		}

		return connector;
	}
}
