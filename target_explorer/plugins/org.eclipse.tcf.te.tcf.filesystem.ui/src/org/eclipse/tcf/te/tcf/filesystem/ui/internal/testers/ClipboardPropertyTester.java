/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.testers;

import java.util.List;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations.FsClipboard;
/**
 * Provide a tester to test if the paste operation is enabled.
 */
public class ClipboardPropertyTester extends PropertyTester {

	/**
	 * Create an instance.
	 */
	public ClipboardPropertyTester() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		Assert.isTrue(receiver instanceof IStructuredSelection);
		if (property.equals("canPaste")) { //$NON-NLS-1$
			FsClipboard cb = UIPlugin.getClipboard();
			if (!cb.isEmpty()) {
				List<IFSTreeNode> nodes = cb.getFiles();
				boolean moving = cb.isCutOp();
				boolean copying = cb.isCopyOp();
				List<IFSTreeNode> selection = ((IStructuredSelection) receiver).toList();
				IFSTreeNode hovered = null;
				Assert.isTrue(!selection.isEmpty());
				if (selection.size() == 1) {
					IFSTreeNode node = selection.get(0);
					if (node.isDirectory() && moving) {
						hovered = node;
					}
					else if (node.isRootDirectory()) {
						hovered = node;
					}
					else {
						hovered = node.getParent();
					}
				}
				else {
					for (IFSTreeNode node : selection) {
						if (hovered == null) hovered = node.getParent();
						else if (hovered != node.getParent()) return false;
					}
				}
				if (hovered != null && hovered.isDirectory() && hovered.isWritable() && (moving || copying)) {
					IFSTreeNode head = nodes.get(0);
					String hid = head.getPeerNode().getPeerId();
					String tid = hovered.getPeerNode().getPeerId();
					if (hid.equals(tid)) {
						for (IFSTreeNode node : nodes) {
							if (moving && node.getParent() == hovered || node.isAncestorOf(hovered)) {
								return false;
							}
						}
						return true;
					}
				}
			}
			else {
				Clipboard clipboard = cb.getSystemClipboard();
				Object contents = clipboard.getContents(FileTransfer.getInstance());
				if (contents != null) {
					List<IFSTreeNode> selection = ((IStructuredSelection) receiver).toList();
					IFSTreeNode hovered = null;
					Assert.isTrue(!selection.isEmpty());
					if (selection.size() == 1) {
						IFSTreeNode node = selection.get(0);
						if (node.isFile()) {
							hovered = node.getParent();
						}
						else {
							hovered = node;
						}
					}
					else {
						for (IFSTreeNode node : selection) {
							if (hovered == null) hovered = node.getParent();
							else if (hovered != node.getParent()) return false;
						}
					}
					if (hovered != null && hovered.isDirectory() && hovered.isWritable()) {
						return true;
					}
					return false;
				}
			}
		}
		return false;
	}
}
