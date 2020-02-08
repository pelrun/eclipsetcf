/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.filesystem.adapters;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AdaptersTests {
	public static Test suite() {
		TestSuite suite = new TestSuite("File System: Adapters Tests"); //$NON-NLS-1$
		suite.addTestSuite(FSTreeNodeAdapterFactoryTest.class);
		suite.addTestSuite(NodeStateFilterTest.class);
		suite.addTestSuite(PeerNodeViewerInputTest.class);
		suite.addTestSuite(ViewerInputAdapterFactoryTest.class);
		return suite;
	}
}
