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

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpCreateFile;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpCreateFolder;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpDelete;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpRename;
import org.eclipse.tcf.te.tests.tcf.filesystem.FSPeerTestCase;

public class OperationTestBase extends FSPeerTestCase {

	protected FSTreeNode copy(FSTreeNode file, FSTreeNode folder) throws Exception {
		printDebugMessage("Copy " + file.getLocation() + " to " + folder.getLocation() + "..."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		List<IFSTreeNode> files = Collections.<IFSTreeNode>singletonList(file);
		IOperation copy = folder.operationDropCopy(files, false, false, null);
		copy.run(new NullProgressMonitor());
		String location = folder.getLocation();
		String path = location + getPathSep() + file.getName();
		return getFSNode(path);
	}

	protected FSTreeNode createFile(String fileName, FSTreeNode folder) throws Exception {
		printDebugMessage("Create " + fileName + " at " + folder.getLocation()); //$NON-NLS-1$ //$NON-NLS-2$
		OpCreateFile create = new OpCreateFile(folder, fileName);
		create.run(new NullProgressMonitor());
		String location = folder.getLocation();
		String path = location + getPathSep() + fileName;
		return getFSNode(path);
	}

	protected FSTreeNode createFolder(String folderName, FSTreeNode folder) throws Exception {
		printDebugMessage("Create " + folderName + " at " + folder.getLocation()); //$NON-NLS-1$ //$NON-NLS-2$
		OpCreateFolder create = new OpCreateFolder(folder, folderName);
		create.run(new NullProgressMonitor());
		String location = folder.getLocation();
		String path = location + getPathSep() + folderName;
		return getFSNode(path);
	}

	protected FSTreeNode move(FSTreeNode src, FSTreeNode dest) throws Exception {
		printDebugMessage("Move " + src.getLocation() + " to " + dest.getLocation()); //$NON-NLS-1$ //$NON-NLS-2$
		List<IFSTreeNode> nodes = Collections.<IFSTreeNode>singletonList(src);
		IOperation fsmove = dest.operationDropMove(nodes, null);
		fsmove.run(new NullProgressMonitor());
		String path = dest.getLocation() + getPathSep() + src.getName();
		return getFSNode(path);
	}

	protected FSTreeNode rename(FSTreeNode node, String newName) throws Exception {
		printDebugMessage("Rename " + node.getName() + " to " + newName); //$NON-NLS-1$ //$NON-NLS-2$
		OpRename fsmove = new OpRename(node, newName);
		fsmove.run(new NullProgressMonitor());
		String newPath = node.getParent().getLocation()+getPathSep()+newName;
		return getFSNode(newPath);
	}

	protected void updateCache(FSTreeNode testFile) throws Exception {
		IOperation update = testFile.operationDownload(null);
		update.run(new NullProgressMonitor());
	}

	protected void commitCache(FSTreeNode testFile) throws Exception {
		IOperation commit = testFile.operationUploadContent(null);
		commit.run(new NullProgressMonitor());
	}

	protected void delete(FSTreeNode node) throws Exception {
		printDebugMessage("Delete " + node.getLocation() + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		List<IFSTreeNode> files = Collections.<IFSTreeNode>singletonList(node);
		OpDelete delete = new OpDelete(files, null);
		delete.run(new NullProgressMonitor());
	}
}
