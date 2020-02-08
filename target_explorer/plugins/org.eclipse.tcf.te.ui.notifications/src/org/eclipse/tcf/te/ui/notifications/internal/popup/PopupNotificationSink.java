/*******************************************************************************
 * Copyright (c) 2004, 2014 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
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
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.tcf.te.runtime.events.NotifyEvent;
import org.eclipse.tcf.te.ui.notifications.nls.Messages;
import org.eclipse.ui.PlatformUI;

/**
 * @author Rob Elves
 * @author Steffen Pingel
 */
public class PopupNotificationSink {

	private static final long DELAY_OPEN = 1 * 1000;

	private static final boolean runSystem = true;

	/* default */ NotificationPopup popup;

	/* default */ final Set<NotifyEvent> currentlyNotifying = Collections.synchronizedSet(new HashSet<NotifyEvent>());

	private final Job openJob = new Job(Messages.PopupNotificationSink_Popup_Notifier_Job_Label) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (Platform.isRunning() && PlatformUI.getWorkbench() != null
							&& PlatformUI.getWorkbench().getDisplay() != null
							&& !PlatformUI.getWorkbench().getDisplay().isDisposed()) {
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {
						synchronized (PopupNotificationSink.class) {
							if (currentlyNotifying.size() > 0) {
								showPopup();
							}
						}
					}
				});
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

	/**
	 * Notify the given notification events.
	 *
	 * @param events The notification events. Must not be <code>null</code>.
	 */
	public void notify(NotifyEvent[] events) {
		Assert.isNotNull(events);

		synchronized (PopupNotificationSink.class) {
			currentlyNotifying.addAll(Arrays.asList(events));
		}

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
		if (popup != null) popup.close();

		if (PlatformUI.isWorkbenchRunning() && PlatformUI.getWorkbench() != null
				&& PlatformUI.getWorkbench().getDisplay() != null && !PlatformUI.getWorkbench().getDisplay().isDisposed()) {
			popup = new NotificationPopup(PlatformUI.getWorkbench().getDisplay().getActiveShell());
			List<NotifyEvent> toDisplay = new ArrayList<NotifyEvent>(currentlyNotifying);
			synchronized (PopupNotificationSink.class) {
				currentlyNotifying.clear();
			}
			Collections.sort(toDisplay);
			popup.setContents(toDisplay);
			popup.setBlockOnOpen(false);
			popup.open();
		}
	}

}
