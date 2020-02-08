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

import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IResultOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;

/**
 * The wizard to create a new file in the file system of Target Explorer.
 */
public class NewFileWizard extends NewNodeWizard {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.wizards.NewNodeWizard#createWizardPage()
	 */
	@Override
	protected NewNodeWizardPage createWizardPage() {
		return new NewFileWizardPage();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.wizards.NewNodeWizard#getCreateOp(org.eclipse.tcf.te.tcf.filesystem.model.IFSTreeNode, java.lang.String, org.eclipse.tcf.te.runtime.interfaces.callback.ICallback)
	 */
	@Override
	protected IResultOperation<? extends IFSTreeNode> getCreateOp(IFSTreeNode folder, String name) {
		return folder.operationNewFile(name);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.wizards.NewNodeWizard#getTitle()
	 */
	@Override
	protected String getTitle() {
		return Messages.NewFileWizard_NewFileWizardTitle;
	}
}
