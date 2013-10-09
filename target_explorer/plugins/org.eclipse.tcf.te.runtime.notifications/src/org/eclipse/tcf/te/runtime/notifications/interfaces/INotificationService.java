/*******************************************************************************
 * Copyright (c) 2010, 2013 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Wind River Systems - Extracted from o.e.mylyn.commons and adapted for Target Explorer
 *******************************************************************************/

package org.eclipse.tcf.te.runtime.notifications.interfaces;

import org.eclipse.tcf.te.runtime.notifications.AbstractNotification;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;

/**
 * @author Steffen Pingel
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface INotificationService extends IService {

	/**
	 * Single notification.
	 *
	 * @param notification The notification. Must not be <code>null</code>.
	 */
	public void notify(AbstractNotification notification);

	/**
	 * Multi notification.
	 *
	 * @param notifications The notifications. Must not be <code>null</code>.
	 */
	public void notify(AbstractNotification[] notifications);

}
