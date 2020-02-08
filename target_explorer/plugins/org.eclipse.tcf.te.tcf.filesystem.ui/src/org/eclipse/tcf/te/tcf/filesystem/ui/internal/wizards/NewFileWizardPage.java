/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.wizards;

import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;

/**
 * The wizard page to create a new file in the file system of Target Explorer.
 */
public class NewFileWizardPage extends NewNodeWizardPage {

	/**
	 * Create a wizard page to create a new file in the specified folder.
	 */
	public NewFileWizardPage() {
		super("NewFileWizardPage"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.wizards.NewNodeWizardPage#getPageTitle()
	 */
	@Override
	protected String getPageTitle() {
		return Messages.NewFileWizardPage_NewFileWizardPageTitle;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.wizards.NewNodeWizardPage#getPageDescription()
	 */
	@Override
	protected String getPageDescription() {
		return Messages.NewFileWizardPage_NewFileWizardPageDescription;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.internal.wizards.NewNodeWizardPage#getNameFieldLabel()
	 */
	@Override
	protected String getNameFieldLabel() {
		return Messages.NewFileWizardPage_NewFileWizardPageNameLabel;
	}
}
