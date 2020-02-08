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

/**
 * UI service.
 * <p>
 * Allows to provide customized implementations for UI related functionality which
 * supports delegating parts of the UI logic to context specific delegates.
 */
public interface IUIService extends IService {

	/**
	 * Returns the delegate instance for the requested UI delegate class and context.
	 * <p>
	 * <b>Note:</b> This method should be used for UI related delegates. For non-UI
	 * related delegates, consider to use the {@link IDelegateService} instead.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @param clazz The delegate class. Must not be <code>null</code>.
	 *
	 * @return The delegate instance or <code>null</code>.
	 */
	public <V extends Object> V getDelegate(Object context, Class<? extends V> clazz);
}
