/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
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
import org.eclipse.tcf.te.tests.activator.ImageConsts;

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
		assertNotNull("Failed to create properties container.", properties); //$NON-NLS-1$

		// First test is using the default form text factory delegate
		properties.setProperty(NotifyEvent.PROP_TITLE_TEXT, "Notification Test"); //$NON-NLS-1$
		properties.setProperty(NotifyEvent.PROP_DESCRIPTION_TEXT, "Test notification with a simple title and description."); //$NON-NLS-1$

		NotifyEvent notification = new NotifyEvent(NotificationsTestCase.this, properties);
		assertNotNull("Failed to create test notification event.", notification); //$NON-NLS-1$

		EventManager.getInstance().fireEvent(notification);

		ExecutorsUtil.waitAndExecute(20000, null);

		// Second test is using a custom form text factory delegate to set an icon
		properties.clearProperties();
		properties.setProperty(NotifyEvent.PROP_TITLE_TEXT, "Simulator Target"); //$NON-NLS-1$
		properties.setProperty(NotifyEvent.PROP_TITLE_IMAGE_ID, ImageConsts.PEER);
		properties.setProperty(NotifyEvent.PROP_DESCRIPTION_TEXT, "Test notification issued by the unit test framework."); //$NON-NLS-1$

		notification = new NotifyEvent(NotificationsTestCase.this, "org.eclipse.tcf.te.tests.delegates.TestFormTextFactoryDelegate", properties); //$NON-NLS-1$
		assertNotNull("Failed to create test notification event.", notification); //$NON-NLS-1$

		EventManager.getInstance().fireEvent(notification);

		ExecutorsUtil.waitAndExecute(20000, null);

		// Third and forth test is using a custom form text factory delegate to simulate a more complex rendering
		properties.clearProperties();
		properties.setProperty(NotifyEvent.PROP_TITLE_TEXT, "Simulator Target"); //$NON-NLS-1$
		properties.setProperty(NotifyEvent.PROP_TITLE_IMAGE_ID, ImageConsts.PEER);
		properties.setProperty("moduleName", "cobble.out"); //$NON-NLS-1$ //$NON-NLS-2$
		properties.setProperty("success", true); //$NON-NLS-1$

		notification = new NotifyEvent(NotificationsTestCase.this, "org.eclipse.tcf.te.tests.delegates.TestFormTextFactoryDelegate2", properties); //$NON-NLS-1$
		assertNotNull("Failed to create test notification event.", notification); //$NON-NLS-1$

		EventManager.getInstance().fireEvent(notification);

		ExecutorsUtil.waitAndExecute(20000, null);

		properties.clearProperties();
		properties.setProperty(NotifyEvent.PROP_TITLE_TEXT, "Simulator Target"); //$NON-NLS-1$
		properties.setProperty(NotifyEvent.PROP_TITLE_IMAGE_ID, ImageConsts.PEER);
		properties.setProperty("moduleName", "cobble.out"); //$NON-NLS-1$ //$NON-NLS-2$
		properties.setProperty("success", false); //$NON-NLS-1$

		notification = new NotifyEvent(NotificationsTestCase.this, "org.eclipse.tcf.te.tests.delegates.TestFormTextFactoryDelegate2", properties); //$NON-NLS-1$
		assertNotNull("Failed to create test notification event.", notification); //$NON-NLS-1$

		EventManager.getInstance().fireEvent(notification);

		ExecutorsUtil.waitAndExecute(20000, null);
	}

	//***** END SECTION: Single test methods *****

}
