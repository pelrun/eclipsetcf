/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Freescale Semiconductor - Bug 293618, Breakpoints view sorts up to first colon only
 *******************************************************************************/
package org.eclipse.tcf.debug.test;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
  * Tests for integration and nightly builds.
 *
 * @since 3.6
 */
public class AutomatedSuite extends TestSuite {

    /**
     * Returns the suite.  This is required to use the JUnit Launcher.
     *
     * @return the test suite
     */
    public static Test suite() {
        return new AutomatedSuite();
    }

    /**
     * Constructs the automated test suite. Adds all tests.
     */
    public AutomatedSuite() {
        addTest(new TestSuite(BreakpointsTest.class));
        addTest(new TestSuite(TransactionTests.class));
        addTest(new TestSuite(BreakpointDetailPaneTest.class));
        addTest(new TestSuite(BreakpointsViewTest.class));
        addTest(new TestSuite(RunControlCMTest.class));
        addTest(new TestSuite(StackTraceCMTest.class));
        addTest(new TestSuite(CacheTests.class));
        addTest(new TestSuite(RangeCacheTests.class));
        addTest(new TestSuite(QueryTests.class));
        addTest(new TestSuite(SampleTest.class));
    }
}
