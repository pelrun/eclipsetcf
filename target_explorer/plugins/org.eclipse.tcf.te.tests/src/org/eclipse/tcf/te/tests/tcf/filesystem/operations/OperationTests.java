/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.filesystem.operations;

import junit.framework.Test;
import junit.framework.TestSuite;

public class OperationTests {
	public static Test suite() {
		TestSuite suite = new TestSuite("File System: Operation Tests"); //$NON-NLS-1$
		suite.addTestSuite(FSCopyTests.class);
		suite.addTestSuite(FSCreateFileTests.class);
		suite.addTestSuite(FSCreateFolderTests.class);
		suite.addTestSuite(FSDeleteTests.class);
		suite.addTestSuite(FSMoveTests.class);
		suite.addTestSuite(FSRefreshTests.class);
		suite.addTestSuite(FSRenameTests.class);
		suite.addTestSuite(FSUploadTest.class);
		suite.addTestSuite(FSCacheCommitTest.class);
		suite.addTestSuite(FSCacheUpdateTest.class);
		return suite;
	}
}
