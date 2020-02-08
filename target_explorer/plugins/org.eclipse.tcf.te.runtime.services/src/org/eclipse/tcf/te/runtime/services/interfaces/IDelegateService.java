/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.services.interfaces;

/**
 * Delegate service.
 * <p>
 * Allows to return specific delegate implementations for a given context.
 * <p>
 * A delegate extends the implementation of a service or method to be context specific.
 */
public interface IDelegateService extends IService {

	/**
	 * Returns the delegate instance for the requested delegate class and context.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param clazz The delegate class. Must not be <code>null</code>.
	 *
	 * @return The delegate instance or <code>null</code>.
	 */
	public <V extends Object> V getDelegate(Object context, Class<? extends V> clazz);
}
