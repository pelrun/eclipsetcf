/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.core.interfaces;

import org.eclipse.tcf.services.IPathMap;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;

/**
 * Provides a set of generated path map rules.
 * <p>
 * Auto generated path map rules typically differs between target types and
 * are typically not to be editable for the user. The path map rules provided
 * by this service are not necessarily persisted.
 */
public interface IPathMapGeneratorService extends IService {

	/**
	 * Return the generated object path mappings for the given context.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @return The generated object path map or <code>null</code>.
	 */
	public IPathMap.PathMapRule[] getPathMap(Object context);

	/**
	 * Return the generated source path mappings for the given context.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @return The generated source path map or <code>null</code>.
	 */
	public IPathMap.PathMapRule[] getSourcePathMap(Object context);
}
