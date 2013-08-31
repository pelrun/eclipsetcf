/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.interfaces;

import org.eclipse.tcf.services.IPathMap;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;

/**
 * Path map service.
 * <p>
 * Allow the access to the configured path maps for a given context.
 */
public interface IPathMapService extends IService {

	/**
	 * Return the configured (object) path mappings for the given context.
	 * <p>
	 * <b>Note:</b> This method must be called from outside the TCF event dispatch thread.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @return The configured path map or <code>null</code>.
	 */
	public IPathMap.PathMapRule[] getPathMap(Object context);

	/**
	 * Returns the current client ID used to identify path map rules handled
	 * by the current Eclipse instance.
	 *
	 * @return The current client ID.
	 */
	public String getClientID();
}
