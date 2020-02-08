/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.filesystem.testers;

import org.eclipse.tcf.te.runtime.utils.Host;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.testers.TargetPropertyTester;
import org.eclipse.tcf.te.tests.tcf.filesystem.FSPeerTestCase;

public class TargetPropertyTesterTest extends FSPeerTestCase {
	public void testIsWindows() {
        TargetPropertyTester tester = new TargetPropertyTester();
		boolean value = tester.test(peerNode, "isWindows", null, null); //$NON-NLS-1$
		if(Host.isWindowsHost()) {
			assertTrue(value);
		}
		else {
			assertFalse(value);
		}
	}
}
