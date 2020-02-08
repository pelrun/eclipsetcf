/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.notifications.interfaces;

import org.eclipse.tcf.te.runtime.events.NotifyEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;


/**
 * Interface to be implemented by notification form text factory delegates.
 */
public interface IFormTextFactoryDelegate {

	/**
	 * Populate the given form text widget based on the given notification event.
	 * <p>
	 * See {@link FormText} for details.
	 *
	 * @param toolkit The form toolkit. Must not be <code>null</code>.
	 * @param widget The form text widget. Must not be <code>null</code>.
	 * @param event The notification event. Must not be <code>null</code>.
	 */
	public void populateFormText(FormToolkit toolkit, FormText widget, NotifyEvent event);

	/**
	 * Returns the delay in milliseconds until to auto-close the notification popup.
	 *
	 * @return The delay in milliseconds or <code>-1</code> to apply the default delay.
	 */
	public long getNotificationCloseDelay();
}
