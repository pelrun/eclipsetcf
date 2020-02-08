/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.core.nodes.interfaces.wire;

/**
 * The properties specific to the wire type &quot;network&quot;.
 */
public interface IWireTypeNetwork {

	/**
	 * The data container.
	 */
	public static String PROPERTY_CONTAINER_NAME = "network"; //$NON-NLS-1$

	/**
	 * The network address.
	 */
	public static final String PROPERTY_NETWORK_ADDRESS = "address"; //$NON-NLS-1$

	/**
	 * The network port.
	 */
	public static final String PROPERTY_NETWORK_PORT = "port"; //$NON-NLS-1$

	/**
	 * The network port is a "auto port" (automatically determined and read-only to the user).
	 */
	public static final String PROPERTY_NETWORK_PORT_IS_AUTO = "autoPort"; //$NON-NLS-1$
}
