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

import java.util.EventListener;
import java.util.EventObject;

/**
 * Interface to be implemented by event listeners.
 */
public interface IEventListener extends EventListener{

	/**
	 * Invoked by the event manager if the event listener registration
	 * applies for the given event object.
	 *
	 * @param event The event. Must not be <code>null</code>.
	 */
	public void eventFired(EventObject event);
}
