/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.dnd.CommonDnD;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations.FsClipboard;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations.UiExecutor;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * The handler that pastes the files or folders in the clip board.
 */
public class PasteFilesHandler extends AbstractHandler {

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		FsClipboard cb = UIPlugin.getClipboard();
		if (!cb.isEmpty()) {
			// Get the files/folders from the clip board.
			IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelectionChecked(event);
			List<IFSTreeNode> nodes = cb.getFiles();
			IOperation operation;
			if (cb.isCutOp()) {
				IFSTreeNode dest = (IFSTreeNode) selection.getFirstElement();
				operation = dest.operationDropMove(nodes, new MoveCopyCallback());
			} else if (cb.isCopyOp()) {
				IFSTreeNode hovered = (IFSTreeNode) selection.getFirstElement();
				IFSTreeNode dest = getCopyDestination(hovered, nodes);
				boolean cpPerm = UIPlugin.isCopyPermission();
				boolean cpOwn = UIPlugin.isCopyOwnership();
				operation = dest.operationDropCopy(nodes, cpPerm, cpOwn, new MoveCopyCallback());
			} else {
				return null;
			}
			UiExecutor.execute(operation);
			if (cb.isCutOp()) {
				UIPlugin.getClipboard().clear();
			}
		} else {
			Clipboard clipboard = cb.getSystemClipboard();
			Object contents = clipboard.getContents(FileTransfer.getInstance());
			if (contents != null) {
				String[] files = (String[]) contents;
				// Get the files/folders from the clip board.
				IStructuredSelection selection = (IStructuredSelection) HandlerUtil
				                .getCurrentSelectionChecked(event);
				IFSTreeNode hovered = (IFSTreeNode) selection.getFirstElement();
				CommonDnD dnd = new CommonDnD();
				dnd.dropFiles(null, files, DND.DROP_COPY, hovered);
			}
		}
		return null;
	}

	/**
	 * Return an appropriate destination directory for copying according to
	 * the specified hovered node.  If the hovered node is a file, then return
	 * its parent directory. If the hovered node is a directory, then return its
	 * self if it is not a node being copied. Return its parent directory if it is
	 * a node being copied.
	 * @param hovered
	 * @param nodes
	 * @return
	 */
	private IFSTreeNode getCopyDestination(IFSTreeNode hovered, List<IFSTreeNode> nodes) {
		if (hovered.isFile()) {
			return hovered.getParent();
		}
		else if (hovered.isDirectory()) {
			for (IFSTreeNode node : nodes) {
				if (node == hovered) {
					return hovered.getParent();
				}
			}
		}
		return hovered;
	}
}
