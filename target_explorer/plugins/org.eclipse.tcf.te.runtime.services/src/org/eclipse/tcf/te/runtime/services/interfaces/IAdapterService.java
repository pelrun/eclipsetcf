/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.services.interfaces;

import org.eclipse.core.runtime.IAdaptable;

/**
 * Adapter service.
 * <p>
 * Allows to return specific adapter implementations for a given context.
 * <p>
 * An adapter is extending the given context object with the desired adapter class.
 * The adapter service is an context specific extension to the core Eclipse adaptable
 * mechanism. See {@link IAdaptable} for details on the core Eclipse adaptable mechanism.
 */
public interface IAdapterService extends IService {

	/**
	 * Returns an adapter for the requested adapter class and context.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param clazz The adapter class. Must not be <code>null</code>.
	 *
	 * @return The adapter instance or <code>null</code>.
	 */
	public <V extends Object> V getAdapter(Object context, Class<? extends V> clazz);
}
