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

import java.io.File;

import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;

public class FSUploadTest extends OperationTestBase {

	@Override
    protected void setUp() throws Exception {
	    super.setUp();
		String targetFile = getTestRoot() + getPathSep() + getTestPath() + getPathSep() + "upload.txt"; //$NON-NLS-1$
		if(pathExists(targetFile)) {
			FSTreeNode targetNode = getFSNode(targetFile);
			assertNotNull(targetNode);
			delete(targetNode);
		}
		String sourceFile = getTestRoot() + getPathSep() + "upload.txt"; //$NON-NLS-1$
		File srcFile = new File(sourceFile);
		if(!srcFile.exists()) {
			srcFile.createNewFile();
		}
    }

	public void testUpload() {

	}
}
