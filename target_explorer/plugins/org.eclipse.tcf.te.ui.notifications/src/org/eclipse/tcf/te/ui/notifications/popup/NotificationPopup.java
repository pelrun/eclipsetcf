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

package org.eclipse.tcf.te.ui.notifications.popup;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.runtime.notifications.AbstractNotification;
import org.eclipse.tcf.te.ui.notifications.ScalingHyperlink;
import org.eclipse.tcf.te.ui.notifications.activator.UIPlugin;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

/**
 * @author Rob Elves
 * @author Mik Kersten
 */
public class NotificationPopup extends AbstractNotificationPopup {

	private static final int NUM_NOTIFICATIONS_TO_DISPLAY = 4;

	/* default */ Color hyperlinkWidget = null;

	private List<AbstractNotification> notifications;

	/**
	 * Constructor
	 *
	 * @param parent The parent shell. Must not be <code>null</code>.
	 */
	public NotificationPopup(Shell parent) {
		super(parent.getDisplay());
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
		for (final AbstractNotification notification : notifications) {
			Composite notificationComposite = new Composite(parent, SWT.NO_FOCUS);
			GridLayout gridLayout = new GridLayout(2, false);
			GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(notificationComposite);
			notificationComposite.setLayout(gridLayout);
			notificationComposite.setBackground(parent.getBackground());

			if (count < NUM_NOTIFICATIONS_TO_DISPLAY) {
				final Label notificationLabelIcon = new Label(notificationComposite, SWT.NO_FOCUS);
				notificationLabelIcon.setBackground(parent.getBackground());
				if (notification instanceof AbstractUiNotification) {
					notificationLabelIcon.setImage(((AbstractUiNotification) notification).getNotificationKindImage());
				}

				final ScalingHyperlink itemLink = new ScalingHyperlink(notificationComposite, SWT.BEGINNING | SWT.NO_FOCUS);
				GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(itemLink);
				itemLink.setForeground(hyperlinkWidget);
				itemLink.registerMouseTrackListener();
				itemLink.setText(LegacyActionTools.escapeMnemonics(notification.getLabel()));
				if (notification instanceof AbstractUiNotification) {
					itemLink.setImage(((AbstractUiNotification) notification).getNotificationImage());
				}
				itemLink.setBackground(parent.getBackground());
				itemLink.addHyperlinkListener(new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
						if (notification instanceof AbstractUiNotification) {
							((AbstractUiNotification) notification).open();
						}
						IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
						if (window != null) {
							Shell windowShell = window.getShell();
							if (windowShell != null) {
								if (windowShell.getMinimized()) {
									windowShell.setMinimized(false);
								}

								windowShell.open();
								windowShell.forceActive();
							}
						}
					}
				});

				String descriptionText = null;
				if (notification.getDescription() != null) {
					descriptionText = notification.getDescription();
				}
				if (descriptionText != null && !descriptionText.trim().equals("")) { //$NON-NLS-1$
					Label descriptionLabel = new Label(notificationComposite, SWT.NO_FOCUS);
					descriptionLabel.setText(LegacyActionTools.escapeMnemonics(descriptionText));
					descriptionLabel.setBackground(parent.getBackground());
					GridDataFactory.fillDefaults()
							.span(2, SWT.DEFAULT)
							.grab(true, false)
							.align(SWT.FILL, SWT.TOP)
							.applyTo(descriptionLabel);
				}
			} else {
				int numNotificationsRemain = notifications.size() - count;
				ScalingHyperlink remainingLink = new ScalingHyperlink(notificationComposite, SWT.NO_FOCUS);
				remainingLink.setForeground(hyperlinkWidget);
				remainingLink.registerMouseTrackListener();
				remainingLink.setBackground(parent.getBackground());

				remainingLink.setText(NLS.bind("{0} more", Integer.valueOf(numNotificationsRemain))); //$NON-NLS-1$
				GridDataFactory.fillDefaults().span(2, SWT.DEFAULT).applyTo(remainingLink);
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

	public List<AbstractNotification> getNotifications() {
		return new ArrayList<AbstractNotification>(notifications);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.notifications.popup.AbstractNotificationPopup#getTitleForeground()
	 */
	@Override
	protected Color getTitleForeground() {
		return UIPlugin.getDefault().getFormColors().getColor(IFormColors.TITLE);
	}

	public void setContents(List<AbstractNotification> notifications) {
		this.notifications = notifications;
	}

}
