/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.wizards;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.celleditor.FSCellValidator;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.tcf.te.ui.controls.validator.Validator;

/**
 * The validator to validate the name of a file/folder in the file system of Target Explorer.
 *
 * @see Validator
 */
public class NameValidator extends Validator {
	// The folder in which the new file/folder is to be created.
	NewNodeWizardPage wizard;

	/**
	 * Create a NameValidator with the folder in which the file/folder is created.
	 *
	 * @param wizard The parent folder in which the file/folder is created.
	 */
	public NameValidator(NewNodeWizardPage wizard) {
		super(ATTR_MANDATORY);
		this.wizard = wizard;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.validator.Validator#isValid(java.lang.String)
	 */
	@Override
	public boolean isValid(String newText) {
		IFSTreeNode folder = wizard.getInputDir();
		if(folder == null) {
			setMessage(Messages.NameValidator_SpecifyFolder, IMessageProvider.INFORMATION);
			return false;
		}
		if (newText == null || newText.trim().length() == 0) {
			setMessage(Messages.FSRenamingAssistant_SpecifyNonEmptyName, IMessageProvider.ERROR);
			return false;
		}
		String text = newText.trim();
		if (hasChild(text)) {
			setMessage(Messages.FSRenamingAssistant_NameAlreadyExists, IMessageProvider.ERROR);
			return false;
		}
		String formatRegex = folder.isWindowsNode() ? FSCellValidator.WIN_FILENAME_REGEX : FSCellValidator.UNIX_FILENAME_REGEX;
		if (!text.matches(formatRegex)) {
			setMessage(folder.isWindowsNode() ? Messages.FSRenamingAssistant_WinIllegalCharacters : Messages.FSRenamingAssistant_UnixIllegalCharacters, IMessageProvider.ERROR);
			return false;
		}
		setMessage(null, IMessageProvider.NONE);
		return true;
	}

	/**
	 * To test if the folder has a child with the specified name.
	 *
	 * @param name The name.
	 * @return true if it has a child with the name.
	 */
	private boolean hasChild(String name) {
		final IFSTreeNode folder = wizard.getInputDir();
		IFSTreeNode[] nodes = folder.getChildren();
		if (nodes == null)
			return false;

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
