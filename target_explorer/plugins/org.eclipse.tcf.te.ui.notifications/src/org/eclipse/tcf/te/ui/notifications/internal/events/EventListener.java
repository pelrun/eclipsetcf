/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.notifications.internal.events;

import java.util.EventObject;

import org.eclipse.tcf.te.runtime.events.NotifyEvent;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.ui.notifications.internal.NotificationService;

/**
 * Event listener implementation. Handle events of type {@link NotifyEvent}.
 */
public class EventListener implements IEventListener {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventListener#eventFired(java.util.EventObject)
	 */
	@Override
	public void eventFired(EventObject event) {
		if (event instanceof NotifyEvent) {
			NotificationService.getInstance().notify((NotifyEvent)event);
		}
	}

}
