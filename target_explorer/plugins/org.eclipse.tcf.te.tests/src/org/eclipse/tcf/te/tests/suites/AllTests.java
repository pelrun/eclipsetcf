/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.suites;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.tests.concurrent.ConcurrentTestCase;
import org.eclipse.tcf.te.tests.model.ModelTestCase;
import org.eclipse.tcf.te.tests.statushandler.StatusHandlerTestCase;
import org.eclipse.tcf.te.tests.stepper.StepperTests;
import org.eclipse.tcf.te.tests.tcf.locator.LocatorModelTestCase;
import org.eclipse.tcf.te.tests.tcf.processes.model.ProcessModelTestCase;
import org.eclipse.tcf.te.tests.utils.UtilityTestCase;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Links all tests together into a single suite.
 */
public class AllTests {

	/**
	 * Main method called if the tests are running as part of the nightly
	 * Workbench wheel. Use only the <code>junit.textui.TestRunner</code>
	 * here to execute the tests!
	 *
	 * @param args The command line arguments passed.
	 */
	public static void main (String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	/**
	 * Static method called by the several possible test runners to fetch
	 * the test(s) to run.
	 * Do not rename this method, otherwise tests will not be called anymore!
	 *
	 * @return Any object of type <code>Test</code> containing the test to run.
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite("All Target Explorer Tests"); //$NON-NLS-1$

		addTests(suite);

		return suite;
	}

	/**
	 * Adds all related tests to the given test suite.
	 *
	 * @param suite The test suite. Must not be <code>null</code>.
	 */
	public static void addTests(TestSuite suite) {
		Assert.isNotNull(suite);

		suite.addTest(ModelTestCase.getTestSuite());
		suite.addTest(org.eclipse.tcf.te.tests.tcf.model.ModelTestCase.getTestSuite());
		suite.addTest(UtilityTestCase.getTestSuite());
		suite.addTest(ConcurrentTestCase.getTestSuite());
		suite.addTest(StatusHandlerTestCase.getTestSuite());
		suite.addTest(LocatorModelTestCase.getTestSuite());

		suite.addTest(StepperTests.getTestSuite());
//		suite.addTest(TcfLaunchTests.getTestSuite());
		suite.addTest(ProcessModelTestCase.getTestSuite());
//		suite.addTest(ProcessLauncherTestCase.getTestSuite());

//		AllFileSystemTests.addTests(suite);
	}
}
