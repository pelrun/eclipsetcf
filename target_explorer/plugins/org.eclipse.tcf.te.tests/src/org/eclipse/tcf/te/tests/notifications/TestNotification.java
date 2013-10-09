/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.notifications;

import java.util.Date;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.ui.notifications.popup.AbstractUiNotification;

/**
 * Test notification implementation.
 */
public class TestNotification extends AbstractUiNotification {
	private String description;
	private String label;

	/**
	 * Constructor
	 *
	 * @param eventId
	 */
	public TestNotification(String eventId) {
		super(eventId);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.notifications.popup.AbstractUiNotification#getNotificationImage()
	 */
	@Override
	public Image getNotificationImage() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.notifications.popup.AbstractUiNotification#getNotificationKindImage()
	 */
	@Override
	public Image getNotificationKindImage() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.notifications.popup.AbstractUiNotification#open()
	 */
	@Override
	public void open() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.notifications.AbstractNotification#getDate()
	 */
	@Override
	public Date getDate() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.notifications.AbstractNotification#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.notifications.AbstractNotification#getLabel()
	 */
	@Override
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

}
