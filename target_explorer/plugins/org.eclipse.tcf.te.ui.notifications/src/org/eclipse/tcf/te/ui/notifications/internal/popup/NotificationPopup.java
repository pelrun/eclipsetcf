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
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.runtime.events.NotifyEvent;
import org.eclipse.tcf.te.ui.notifications.activator.UIPlugin;
import org.eclipse.tcf.te.ui.notifications.interfaces.IFormTextFactoryDelegate;
import org.eclipse.tcf.te.ui.notifications.internal.factory.FactoryDelegateManager;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * @author Rob Elves
 * @author Mik Kersten
 */
public class NotificationPopup extends AbstractNotificationPopup {

	private static final int NUM_NOTIFICATIONS_TO_DISPLAY = 4;

	/* default */ Color hyperlinkWidget = null;

	private List<NotifyEvent> notifications;

	/**
	 * Constructor
	 *
	 * @param parent The parent shell or <code>null</code> to create a top level shell.
	 */
	public NotificationPopup(Shell parent) {
		super(parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.notifications.popup.AbstractNotificationPopup#createContentArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createContentArea(Composite parent) {
		Assert.isNotNull(parent);
		hyperlinkWidget = new Color(parent.getDisplay(), 12, 81, 172);
		parent.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (hyperlinkWidget != null) {
					hyperlinkWidget.dispose();
					hyperlinkWidget = null;
				}
			}
		});

		int count = 0;
		for (final NotifyEvent notification : notifications) {
			Composite notificationComposite = new Composite(parent, SWT.NO_FOCUS);
			notificationComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			notificationComposite.setLayout(new GridLayout(1, false));
			notificationComposite.setBackground(parent.getBackground());

			if (count < NUM_NOTIFICATIONS_TO_DISPLAY) {
				// Get the notification form text factory delegate for the current notification
				IFormTextFactoryDelegate delegate = null;
				if (notification.getFactoryId() != null) {
					delegate = FactoryDelegateManager.getInstance().getFactoryDelegate(notification.getFactoryId());
				}
				if (delegate == null) delegate = FactoryDelegateManager.getInstance().getDefaultFactoryDelegate();
				Assert.isNotNull(delegate);

				// Get the form toolkit to use
				FormToolkit toolkit = UIPlugin.getDefault().getFormToolkit();
				Assert.isNotNull(toolkit);

				// Create the form text widget.
				FormText widget = toolkit.createFormText(notificationComposite, true);
				GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
				layoutData.widthHint = 300;
				widget.setLayoutData(layoutData);
				widget.setBackground(notificationComposite.getBackground());
				widget.setWhitespaceNormalized(false);

				// Associate the notification event with the form text widget
				widget.setData("event", notification); //$NON-NLS-1$

				// Populate the widget content based on the current notification event
				delegate.populateFormText(toolkit, widget, notification);
				// Adjust the notification close delay
				setDelayClose(delegate.getNotificationCloseDelay());
			} else {
				int numNotificationsRemain = notifications.size() - count;
				ScalingHyperlink remainingLink = new ScalingHyperlink(notificationComposite, SWT.NO_FOCUS);
				remainingLink.setForeground(hyperlinkWidget);
				remainingLink.registerMouseTrackListener();
				remainingLink.setBackground(parent.getBackground());

				remainingLink.setText(NLS.bind("{0} more", Integer.valueOf(numNotificationsRemain))); //$NON-NLS-1$
				remainingLink.addHyperlinkListener(new HyperlinkAdapter() {
					@Override
                    public void linkActivated(HyperlinkEvent e) {
						IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
						if (window != null) {
							Shell windowShell = window.getShell();
							if (windowShell != null) {
								windowShell.setMaximized(true);
								windowShell.open();
							}
						}
					}
				});
				break;
			}
			count++;
		}
	}

	public List<NotifyEvent> getNotifications() {
		return new ArrayList<NotifyEvent>(notifications);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.notifications.popup.AbstractNotificationPopup#getTitleForeground()
	 */
	@Override
	protected Color getTitleForeground() {
		return UIPlugin.getDefault().getFormToolkit().getColors().getColor(IFormColors.TITLE);
	}

	/**
	 * Sets the content of the notify popup.
	 *
	 * @param notifications The notification events. Must not be <code>null</code>.
	 */
	public void setContents(List<NotifyEvent> notifications) {
		Assert.isNotNull(notifications);
		this.notifications = notifications;
	}

}
