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
 *     Itema AS - bug 331424 handle default event-sink action associations
 *     Wind River Systems - Extracted from o.e.mylyn.commons and adapted for Target Explorer
 *******************************************************************************/

package org.eclipse.tcf.te.ui.notifications.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Steffen Pingel
 * @author Torkild U. Resheim
 */
public class NotificationModel {

	private Map<String, NotificationHandler> handlerByEventId;

	/**
	 * Constructor
	 */
	public NotificationModel() {
		this.handlerByEventId = new HashMap<String, NotificationHandler>();
		// We need the handlerByEventId map to be populated early
		for (NotificationCategory category : getCategories()) {
			for (NotificationEvent event : category.getEvents()) {
				getOrCreateNotificationHandler(event);
			}
		}
	}

	public Collection<NotificationCategory> getCategories() {
		return NotificationsExtensionReader.getCategories();
	}

	public NotificationHandler getNotificationHandler(String eventId) {
		return handlerByEventId.get(eventId);
	}

	public NotificationHandler getOrCreateNotificationHandler(NotificationEvent event) {
		NotificationHandler handler = getNotificationHandler(event.getId());
		if (handler == null) {
			handler = new NotificationHandler(event, getActions(event));
			handlerByEventId.put(event.getId(), handler);
		}
		return handler;
	}

	private List<NotificationAction> getActions(NotificationEvent event) {
		List<NotificationSinkDescriptor> descriptors = NotificationsExtensionReader.getSinks();
		List<NotificationAction> actions = new ArrayList<NotificationAction>(descriptors.size());
		for (NotificationSinkDescriptor descriptor : descriptors) {
			NotificationAction action = new NotificationAction(descriptor);
			if (event.defaultHandledBySink(descriptor.getId())) {
				action.setSelected(true);
			}
			actions.add(action);
		}
		return actions;
	}

	public boolean isSelected(NotificationEvent event) {
		NotificationHandler handler = getOrCreateNotificationHandler(event);
		for (NotificationAction action : handler.getActions()) {
			if (action.isSelected()) {
				return true;
			}
		}
		return false;
	}
}
