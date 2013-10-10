/*******************************************************************************
 * Copyright (c) 2004, 2013 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Wind River Systems - Extracted from o.e.mylyn.commons and adapted for Target Explorer
 *******************************************************************************/

package org.eclipse.tcf.te.ui.notifications.internal.popup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.runtime.events.NotifyEvent;
import org.eclipse.tcf.te.ui.notifications.nls.Messages;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;

/**
 * @author Rob Elves
 * @author Steffen Pingel
 */
public class PopupNotificationSink {

	private static final long DELAY_OPEN = 1 * 1000;

	private static final boolean runSystem = true;

	/* default */ NotificationPopup popup;

	/* default */ final List<NotifyEvent> cancelledNotifications = new ArrayList<NotifyEvent>();

	private final Set<NotifyEvent> notifications = new HashSet<NotifyEvent>();

	/* default */ final Set<NotifyEvent> currentlyNotifying = Collections.synchronizedSet(notifications);

	private final Job openJob = new Job(Messages.PopupNotificationSink_Popup_Notifier_Job_Label) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				if (Platform.isRunning() && PlatformUI.getWorkbench() != null
						&& PlatformUI.getWorkbench().getDisplay() != null
						&& !PlatformUI.getWorkbench().getDisplay().isDisposed()) {
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

						@Override
                        public void run() {
							if (popup != null && popup.getReturnCode() == Window.CANCEL) {
								List<NotifyEvent> notifications = popup.getNotifications();
								for (NotifyEvent notification : notifications) {
									if (!cancelledNotifications.contains(notification)) {
										cancelledNotifications.add(notification);
									}
								}
							}

							for (Iterator<NotifyEvent> it = currentlyNotifying.iterator(); it.hasNext();) {
								NotifyEvent notification = it.next();
								if (cancelledNotifications.contains(notification)) {
									it.remove();
								}
							}

							synchronized (PopupNotificationSink.class) {
								if (currentlyNotifying.size() > 0) {
									showPopup();
								}
							}
						}
					});
				}
			} finally {
				if (popup != null) {
					schedule(popup.getDelayClose() / 2);
				}
			}

			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			return Status.OK_STATUS;
		}

	};


	public PopupNotificationSink() {
		openJob.setSystem(runSystem);
	}

	private void cleanNotified() {
		currentlyNotifying.clear();
	}


	public boolean isAnimationsEnabled() {
		IPreferenceStore store = PlatformUI.getPreferenceStore();
		return store.getBoolean(IWorkbenchPreferenceConstants.ENABLE_ANIMATIONS);
	}

	/**
	 * Notify the given notification events.
	 *
	 * @param events The notification events. Must not be <code>null</code>.
	 */
	public void notify(NotifyEvent[] events) {
		Assert.isNotNull(events);

		currentlyNotifying.addAll(Arrays.asList(events));

		if (!openJob.cancel()) {
			try {
				openJob.join();
			} catch (InterruptedException e) {
				// ignore
			}
		}
		openJob.schedule(DELAY_OPEN);
	}

	public void showPopup() {
		if (popup != null) {
			popup.close();
		}

		Shell shell = new Shell(PlatformUI.getWorkbench().getDisplay());
		popup = new NotificationPopup(shell);
		popup.setFadingEnabled(isAnimationsEnabled());
		List<NotifyEvent> toDisplay = new ArrayList<NotifyEvent>(currentlyNotifying);
		Collections.sort(toDisplay);
		popup.setContents(toDisplay);
		cleanNotified();
		popup.setBlockOnOpen(false);
		popup.open();
	}

}
