/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.celleditor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;

/**
 * FSCellValidator is an <code>ICellEditorValidator</code> that validates the name input in the file system
 * tree cell editor.
 */
public class FSCellValidator implements ICellEditorValidator {
	// The regular expression to define the pattern of a valid Unix file name(not '/').
	public static final String UNIX_FILENAME_REGEX = "[^/]+"; //$NON-NLS-1$
	// The regular expression to define the pattern of a valid Windows file name.
	// (not '?', '\', '/','*','<','>' and '|').
	public static final String WIN_FILENAME_REGEX = "[^(\\?|\\\\|/|:|\\*|<|>|\\|)]+"; //$NON-NLS-1$

	// The tree viewer used to display the file system.
	private TreeViewer viewer;
	/**
	 * Create an FSCellValidator for the specified file system tree.
	 *
	 * @param viewer The tree viewer for the file system.
	 */
	public FSCellValidator(TreeViewer viewer) {
		this.viewer = viewer;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellEditorValidator#isValid(java.lang.Object)
	 */
	@Override
	public String isValid(Object value) {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		Object element = selection.getFirstElement();
		Assert.isTrue(element instanceof IFSTreeNode);
		IFSTreeNode node = (IFSTreeNode) element;
		if (value == null) return Messages.FSRenamingAssistant_SpecifyNonEmptyName;
		String text = value.toString().trim();
		if (text.length() == 0) return Messages.FSRenamingAssistant_SpecifyNonEmptyName;
		if (hasSibbling(node, text)) {
			return Messages.FSRenamingAssistant_NameAlreadyExists;
		}
		String formatRegex = node.isWindowsNode() ? WIN_FILENAME_REGEX : UNIX_FILENAME_REGEX;
		if (!text.matches(formatRegex)) {
			return node.isWindowsNode() ? Messages.FSRenamingAssistant_WinIllegalCharacters : Messages.FSRenamingAssistant_UnixIllegalCharacters;
		}
		return null;
	}
	/**
	 * To test if the folder has a child with the specified name.
	 *
	 * @param folder The folder node.
	 * @param name The name.
	 * @return true if it has a child with the name.
	 */
	private boolean hasSibbling(IFSTreeNode folder, String name) {
		IFSTreeNode[] nodes = folder.getParent().getChildren();
		if (nodes == null) {
			return false;
		}
		for (IFSTreeNode node : nodes) {
			if (node.isWindowsNode()) {
				if (node.getName().equalsIgnoreCase(name)) {
					return true;
				}
			} else if (node.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}
}
