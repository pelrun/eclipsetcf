/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.interfaces;

import org.eclipse.tcf.te.tcf.core.util.persistence.PeerDataHelper;

/**
 * Custom peer property IDs.
 */
public interface IPeerProperties {

	/**
	 * Property: The list of proxies to use to connect to the target.
	 *           The value of this property is of type <code>String</code> and
	 *           must be decoded using {@link PeerDataHelper#decodePeerList(String)}.
	 */
	public static final String PROP_PROXIES = "Proxies"; //$NON-NLS-1$

	/**
	 * Property: The version of the connection.
	 */
	public static final String PROP_VERSION = "Version";  //$NON-NLS-1$

	/**
	 * Property: <code>true</code> if this connection was already migrated to a higher version.
	 */
	public static final String PROP_MIGRATED = "Migrated";  //$NON-NLS-1$
}
