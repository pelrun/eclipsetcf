/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.filesystem.operations;

import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;


public class FSMoveTests extends OperationTestBase {
	protected FSTreeNode originalFolder;

	public void testMove() throws Exception {
		originalFolder = test22File.getParent();
		test22File = move(test22File, test1Folder);
		String origPath = originalFolder.getLocation() + getPathSep() + test22File.getName();
		assertFalse(pathExists(origPath));
		String nowPath = test1Folder.getLocation() + getPathSep() + test22File.getName();
		assertTrue(pathExists(nowPath));
	}

	@Override
	protected void tearDown() throws Exception {
		move(test22File, originalFolder);
		super.tearDown();
	}
}
