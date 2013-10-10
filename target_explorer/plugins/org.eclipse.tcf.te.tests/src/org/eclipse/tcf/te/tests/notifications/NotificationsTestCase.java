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
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.events.NotifyEvent;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
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
		IPropertiesContainer properties = new PropertiesContainer();
		properties.setProperty(NotifyEvent.PROP_TITLE_TEXT, "VxWorks Simulator"); //$NON-NLS-1$
		properties.setProperty(NotifyEvent.PROP_DESCRIPTION_TEXT, "Test notification issued by the unit test framework."); //$NON-NLS-1$

		NotifyEvent notification = new NotifyEvent(NotificationsTestCase.this, properties);
		assertNotNull("Failed to create test notification event.", notification); //$NON-NLS-1$

		EventManager.getInstance().fireEvent(notification);

		ExecutorsUtil.waitAndExecute(20000, null);
	}

	//***** END SECTION: Single test methods *****

}
