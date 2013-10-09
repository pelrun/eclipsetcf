/*******************************************************************************
 * Copyright (c) 2010, 2013 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Itema AS - bug 330064 notification filtering and model persistence
 *     Wind River Systems - Extracted from o.e.mylyn.commons and adapted for Target Explorer
 *******************************************************************************/

package org.eclipse.tcf.te.ui.notifications.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.runtime.notifications.AbstractNotification;
import org.eclipse.tcf.te.runtime.notifications.NotificationSink;
import org.eclipse.tcf.te.runtime.notifications.NotificationSinkEvent;
import org.eclipse.tcf.te.runtime.notifications.interfaces.INotificationService;
import org.eclipse.tcf.te.runtime.services.AbstractService;
import org.eclipse.tcf.te.ui.notifications.activator.UIPlugin;
import org.eclipse.tcf.te.ui.notifications.preferences.IPreferenceKeys;

/**
 * @author Steffen Pingel
 * @author Torkild U. Resheim
 */
public class NotificationService extends AbstractService implements INotificationService {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.notifications.interfaces.INotificationService#notify(org.eclipse.tcf.te.runtime.notifications.AbstractNotification)
	 */
	@Override
	public void notify(AbstractNotification notification) {
		notify(new AbstractNotification[] { notification });
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.notifications.interfaces.INotificationService#notify(org.eclipse.tcf.te.runtime.notifications.AbstractNotification[])
	 */
	@Override
	public void notify(AbstractNotification[] notifications) {
		// Return if notifications are not globally enabled.
		if (!UIPlugin.getDefault().getPreferenceStore().getBoolean(IPreferenceKeys.PREF_SERVICE_ENABLED)) {
			return;
		}

		// For each sink assemble a list of notifications that are not blocked
		// and pass these along.
		HashMap<NotificationSink, ArrayList<AbstractNotification>> filtered = new HashMap<NotificationSink, ArrayList<AbstractNotification>>();
		for (AbstractNotification notification : notifications) {
			String id = notification.getEventId();
			NotificationHandler handler = UIPlugin.getDefault().getModel().getNotificationHandler(id);
			if (handler != null) {
				List<NotificationAction> actions = handler.getActions();
				for (NotificationAction action : actions) {
					if (action.isSelected()) {
						NotificationSink sink = action.getSinkDescriptor().getSink();
						if (sink != null) {
							ArrayList<AbstractNotification> list = filtered.get(sink);
							if (list == null) {
								list = new ArrayList<AbstractNotification>();
								filtered.put(sink, list);
							}
							list.add(notification);
						}
					}
				}
			}
		}
		// Go through all the sinks that have notifications to display and let
		// them do their job.
		for (Entry<NotificationSink, ArrayList<AbstractNotification>> entry : filtered.entrySet()) {
			final NotificationSink sink = entry.getKey();
			final NotificationSinkEvent event = new NotificationSinkEvent(new ArrayList<AbstractNotification>(
					entry.getValue()));
			SafeRunner.run(new ISafeRunnable() {
				@Override
                public void handleException(Throwable e) {
					UIPlugin.getDefault().getLog().log(new Status(IStatus.WARNING, UIPlugin.getUniqueIdentifier(), "Sink failed: " + sink.getClass(), e)); //$NON-NLS-1$
				}

				@Override
                public void run() throws Exception {
					sink.notify(event);
				}
			});
		}
	}

}
