/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tests.tcf.filesystem.dnd;

import java.io.File;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.dnd.CommonDnD;
import org.eclipse.tcf.te.tests.tcf.filesystem.operations.OperationTestBase;

@SuppressWarnings("restriction")
public class CommonDnDTest extends OperationTestBase {
	private CommonDnD dnd;
    @Override
    protected void setUp() throws Exception {
	    super.setUp();
	    dnd = new CommonDnD();
    }

	public void testIsDraggable() {
		IStructuredSelection selection = new StructuredSelection(testFile);
		assertTrue(dnd.isDraggable(selection));
	}

	public void testDropFiles() throws Exception {
		String targetFile = getTestRoot() + getPathSep() + getTestPath() + getPathSep() + "dnd.txt"; //$NON-NLS-1$
		if(pathExists(targetFile)) {
			FSTreeNode targetNode = getFSNode(targetFile);
			assertNotNull(targetNode);
			delete(targetNode);
		}
		String sourceFile = getTestRoot() + getPathSep() + "dnd.txt"; //$NON-NLS-1$
		File srcFile = new File(sourceFile);
		if(!srcFile.exists()) {
			srcFile.createNewFile();
		}
		assertTrue(dnd.dropFiles(null, new String[]{sourceFile}, DND.DROP_COPY, testFolder));
		assertTrue(pathExists(targetFile));
	}

	public void testDropLocalSelection() {
		FSTreeNode existing = test11Folder.findChild(testFile.getName());
		if (existing != null) {
			existing.operationDelete(null).run(null);
		}
		assertTrue(dnd.dropLocalSelection(test11Folder, DND.DROP_COPY, new StructuredSelection(testFile)));
	}
}
