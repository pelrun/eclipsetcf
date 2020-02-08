/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.notifications.internal;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.te.runtime.events.NotifyEvent;
import org.eclipse.tcf.te.ui.notifications.activator.UIPlugin;
import org.eclipse.tcf.te.ui.notifications.internal.popup.PopupNotificationSink;

/**
 * Notification service implementation.
 */
public class NotificationService {
	// Reference to the popup notification sink we use exclusively
	/* default */ final PopupNotificationSink sink = new PopupNotificationSink();

	/*
	 * Thread save singleton instance creation.
	 */
	private static class LazyInstance {
		public static NotificationService instance = new NotificationService();
	}

	/**
	 * Constructor.
	 */
	NotificationService() {
		super();
	}

	/**
	 * Returns the singleton instance of the notification service.
	 */
	public static NotificationService getInstance() {
		return LazyInstance.instance;
	}

	/**
	 * Shows a single notification.
	 *
	 * @param event The notification event. Must not be <code>null</code>.
	 */
	public void notify(NotifyEvent event) {
		Assert.isNotNull(event);
		notify(new NotifyEvent[] { event });
	}

	/**
	 * Shows a set of notifications.
	 *
	 * @param events The notification events. Must not be <code>null</code>.
	 */
	public void notify(final NotifyEvent[] events) {
		Assert.isNotNull(events);

		SafeRunner.run(new ISafeRunnable() {
			@Override
			public void run() throws Exception {
				sink.notify(events);
			}

			@Override
			public void handleException(Throwable e) {
				UIPlugin.getDefault().getLog().log(new Status(IStatus.WARNING, UIPlugin.getUniqueIdentifier(), "Sink failed: " + sink.getClass(), e)); //$NON-NLS-1$
			}

		});
	}

}
