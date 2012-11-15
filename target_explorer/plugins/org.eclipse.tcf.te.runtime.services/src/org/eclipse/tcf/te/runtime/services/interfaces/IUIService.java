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
 * UI service.
 * <p>
 * Allows to provide customized implementations for UI related functionality which
 * supports delegating parts of the logic to context specific delegates.
 */
public interface IUIService extends IService {

	/**
	 * Returns the delegate for the requested delegate class and context.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param clazz The delegate class. Must not be <code>null</code>.
	 *
	 * @return The delegate or <code>null</code>.
	 */
	public <V extends Object> V getDelegate(Object context, Class<? extends V> clazz);
}
