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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.notifications.interfaces.INotificationService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tests.CoreTestCase;

/**
 * Notification test cases.
 */
public class NotificationsTestCase extends CoreTestCase {
	/**
	 * Provides a test suite to the caller which combines all single
	 * test bundled within this category.
	 *
	 * @return Test suite containing all test for this test category.
	 */
	public static Test getTestSuite() {
		TestSuite testSuite = new TestSuite("Test notifications framework"); //$NON-NLS-1$

			// add ourself to the test suite
			testSuite.addTestSuite(NotificationsTestCase.class);

		return testSuite;
	}

	//***** BEGIN SECTION: Single test methods *****
	//NOTE: All method which represents a single test case must
	//      start with 'test'!

	public void testNotifications() {
		// Get the service
		INotificationService service = ServiceManager.getInstance().getService(INotificationService.class);
		assertNotNull("Failed to get notification service instance.", service); //$NON-NLS-1$

		TestNotification notification = new TestNotification("org.eclipse.tcf.te.tests.event1"); //$NON-NLS-1$
		assertNotNull("Failed to create test notification.", notification); //$NON-NLS-1$

		notification.setLabel("Test Notification Label"); //$NON-NLS-1$
		assertEquals("Notification label setter / getter does not match.", "Test Notification Label", notification.getLabel()); //$NON-NLS-1$ //$NON-NLS-2$

		notification.setDescription("Test Notification Description"); //$NON-NLS-1$
		assertEquals("Notification description setter / getter does not match.", "Test Notification Description", notification.getDescription()); //$NON-NLS-1$ //$NON-NLS-2$

		service.notify(notification);

		ExecutorsUtil.waitAndExecute(20000, null);
	}

	//***** END SECTION: Single test methods *****

}
