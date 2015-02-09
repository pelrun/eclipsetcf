/*******************************************************************************
 * Copyright (c) 2011 - 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.interfaces;

import java.util.Map;

import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;

/**
 * Terminal connector type.
 */
@SuppressWarnings("restriction")
public interface IConnectorType extends IExecutableExtension {

	/**
	 * Returns the unique id of the terminal connector type. The returned
	 * id must be never <code>null</code> or an empty string.
	 *
	 * @return The unique id.
	 */
	public String getId();

	/**
	 * Creates the terminal connector for this terminal connector type
	 * based on the given properties.
	 *
	 * @param properties The terminal properties. Must not be <code>null</code>.
	 * @return The terminal connector or <code>null</code>.
	 */
    public ITerminalConnector createTerminalConnector(Map<String, Object> properties);
}
