/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.serial.types;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.core.terminals.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tcf.te.ui.terminals.internal.SettingsStore;
import org.eclipse.tcf.te.ui.terminals.types.AbstractConnectorType;
import org.eclipse.tm.internal.terminal.provisional.api.ISettingsStore;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.tm.internal.terminal.provisional.api.TerminalConnectorExtension;
import org.eclipse.tm.internal.terminal.serial.SerialSettings;

/**
 * Serial terminal connector type implementation.
 */
@SuppressWarnings("restriction")
public class SerialConnectorType extends AbstractConnectorType {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.IConnectorType#createTerminalConnector(java.util.Map)
	 */
	@Override
	public ITerminalConnector createTerminalConnector(Map<String, Object> properties) {
    	Assert.isNotNull(properties);

    	// Check for the terminal connector id
    	String connectorId = (String)properties.get(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID);
		if (connectorId == null) connectorId = "org.eclipse.tm.internal.terminal.serial.SerialConnector"; //$NON-NLS-1$

		String port = (String)properties.get(ITerminalsConnectorConstants.PROP_SERIAL_DEVICE);
		String baud = (String)properties.get(ITerminalsConnectorConstants.PROP_SERIAL_BAUD_RATE);
		Object value = properties.get(ITerminalsConnectorConstants.PROP_TIMEOUT);
		String timeout = value instanceof Integer ? ((Integer)value).toString() : null;
		String databits = (String)properties.get(ITerminalsConnectorConstants.PROP_SERIAL_DATA_BITS);
		String stopbits = (String)properties.get(ITerminalsConnectorConstants.PROP_SERIAL_STOP_BITS);
		String parity = (String)properties.get(ITerminalsConnectorConstants.PROP_SERIAL_PARITY);
		String flowcontrol = (String)properties.get(ITerminalsConnectorConstants.PROP_SERIAL_FLOW_CONTROL);

		// Construct the terminal settings store
		ISettingsStore store = new SettingsStore();

		// Construct the serial settings
		SerialSettings serialSettings = new SerialSettings();
		serialSettings.setSerialPort(port);
		serialSettings.setBaudRate(baud);
		serialSettings.setTimeout(timeout);
		serialSettings.setDataBits(databits);
		serialSettings.setStopBits(stopbits);
		serialSettings.setParity(parity);
		serialSettings.setFlowControl(flowcontrol);

		// And save the settings to the store
		serialSettings.save(store);

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
