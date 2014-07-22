/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.serial.launcher;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tcf.te.ui.terminals.interfaces.IMementoHandler;
import org.eclipse.ui.IMemento;

/**
 * Serial terminal connection memento handler implementation.
 */
public class SerialMementoHandler implements IMementoHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.IMementoHandler#saveState(org.eclipse.ui.IMemento, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void saveState(IMemento memento, IPropertiesContainer properties) {
		Assert.isNotNull(memento);
		Assert.isNotNull(properties);

		// Do not write the terminal title to the memento -> needs to
		// be recreated at the time of restoration.
		memento.putString(ITerminalsConnectorConstants.PROP_SERIAL_DEVICE, properties.getStringProperty(ITerminalsConnectorConstants.PROP_SERIAL_DEVICE));
		memento.putString(ITerminalsConnectorConstants.PROP_SERIAL_BAUD_RATE, properties.getStringProperty(ITerminalsConnectorConstants.PROP_SERIAL_BAUD_RATE));
		memento.putString(ITerminalsConnectorConstants.PROP_SERIAL_DATA_BITS, properties.getStringProperty(ITerminalsConnectorConstants.PROP_SERIAL_DATA_BITS));
		memento.putString(ITerminalsConnectorConstants.PROP_SERIAL_PARITY, properties.getStringProperty(ITerminalsConnectorConstants.PROP_SERIAL_PARITY));
		memento.putString(ITerminalsConnectorConstants.PROP_SERIAL_STOP_BITS, properties.getStringProperty(ITerminalsConnectorConstants.PROP_SERIAL_STOP_BITS));
		memento.putString(ITerminalsConnectorConstants.PROP_SERIAL_FLOW_CONTROL, properties.getStringProperty(ITerminalsConnectorConstants.PROP_SERIAL_FLOW_CONTROL));
		memento.putInteger(ITerminalsConnectorConstants.PROP_TIMEOUT, properties.getIntProperty(ITerminalsConnectorConstants.PROP_TIMEOUT));
		memento.putString(ITerminalsConnectorConstants.PROP_ENCODING, properties.getStringProperty(ITerminalsConnectorConstants.PROP_ENCODING));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.IMementoHandler#restoreState(org.eclipse.ui.IMemento, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void restoreState(IMemento memento, IPropertiesContainer properties) {
		Assert.isNotNull(memento);
		Assert.isNotNull(properties);

		// Restore the terminal properties from the memento
		properties.setProperty(ITerminalsConnectorConstants.PROP_SERIAL_DEVICE, memento.getString(ITerminalsConnectorConstants.PROP_SERIAL_DEVICE));
		properties.setProperty(ITerminalsConnectorConstants.PROP_SERIAL_BAUD_RATE, memento.getString(ITerminalsConnectorConstants.PROP_SERIAL_BAUD_RATE));
		properties.setProperty(ITerminalsConnectorConstants.PROP_SERIAL_DATA_BITS, memento.getString(ITerminalsConnectorConstants.PROP_SERIAL_DATA_BITS));
		properties.setProperty(ITerminalsConnectorConstants.PROP_SERIAL_PARITY, memento.getString(ITerminalsConnectorConstants.PROP_SERIAL_PARITY));
		properties.setProperty(ITerminalsConnectorConstants.PROP_SERIAL_STOP_BITS, memento.getString(ITerminalsConnectorConstants.PROP_SERIAL_STOP_BITS));
		properties.setProperty(ITerminalsConnectorConstants.PROP_SERIAL_FLOW_CONTROL, memento.getString(ITerminalsConnectorConstants.PROP_SERIAL_FLOW_CONTROL));
		properties.setProperty(ITerminalsConnectorConstants.PROP_TIMEOUT, memento.getInteger(ITerminalsConnectorConstants.PROP_TIMEOUT));
		properties.setProperty(ITerminalsConnectorConstants.PROP_ENCODING, memento.getString(ITerminalsConnectorConstants.PROP_ENCODING));
	}
}
