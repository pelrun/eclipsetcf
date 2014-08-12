/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.locator.interfaces;

import org.eclipse.tcf.protocol.IPeer;
import org.osgi.framework.Version;

/**
 * IPeerModelMigrationDelegate
 */
public interface IPeerModelMigrationDelegate {

	/**
	 * Get the active version for peers handled by this delegate.
	 * @return The active version.
	 */
	public Version getVersion();

	/**
	 * Migrate the given peer to the active version.
	 *
	 * @param peer The peer to migrate.
	 * @return The migrated peer or <code>null</code> if nothing to migrate.
	 */
	public IPeer migrate(IPeer peer);
}
