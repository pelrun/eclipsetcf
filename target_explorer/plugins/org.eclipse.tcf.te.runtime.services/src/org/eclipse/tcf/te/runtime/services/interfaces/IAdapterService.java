/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.services.interfaces;

/**
 * Adapter service.
 * <p>
 * Allows to return specific adapter implementations for a given context.
 */
public interface IAdapterService extends IService {

	/**
	 * Returns an adapter for the requested adapter class and context.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param adapter The adapter class. Must not be <code>null</code>.
	 *
	 * @return The adapter or <code>null</code>.
	 */
	public <V extends Object> V getAdapter(Object context, Class<? extends V> adapter);
}
