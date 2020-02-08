/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.controls.help;

import org.eclipse.tcf.te.ui.controls.activator.UIPlugin;

/**
 * Context help id definitions.
 */
public interface IContextHelpIds {

	/**
	 * UI plug-in common context help id prefix.
	 */
	public final static String PREFIX = UIPlugin.getUniqueIdentifier() + "."; //$NON-NLS-1$

	/**
	 * Serial line configuration custom port or address dialog.
	 */
	public final static String SERIAL_PORT_ADDRESS_DIALOG = PREFIX + "SerialPortAddressDialog"; //$NON-NLS-1$
}
