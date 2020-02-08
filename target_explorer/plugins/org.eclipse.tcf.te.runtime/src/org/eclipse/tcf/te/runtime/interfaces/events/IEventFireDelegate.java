/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.interfaces.events;

/**
 * Common interface for notification fire delegate listeners.
 * <p>
 * If a event listener additionally implements this interface, the event manager will
 * call the {@link #fire(Runnable)} method to delegate the thread handling.
 */
public interface IEventFireDelegate {

	/**
	 * Fire the given runnable. If the given runnable is <code>null</code>, the method should return
	 * immediately. The implementor of the interface is responsible for the thread-handling.
	 *
	 * @param runnable The runnable that should be started for notification or <code>null</code>.
	 */
	public void fire(final Runnable runnable);
}
