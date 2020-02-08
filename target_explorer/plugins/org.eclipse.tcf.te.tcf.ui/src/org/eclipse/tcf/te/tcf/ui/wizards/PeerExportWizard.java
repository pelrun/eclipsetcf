/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.ui.wizards;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.tcf.te.tcf.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.ui.wizards.pages.PeerExportWizardPage;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

/**
 * PeerExportWizard
 */
public class PeerExportWizard extends Wizard implements IExportWizard {

	private String EXPORT_DIALOG_SETTINGS = "PeerExport"; //$NON-NLS-1$

	private IStructuredSelection fSelection = null;

	/**
	 * Constructor.
	 */
	public PeerExportWizard() {
		super();
		UIPlugin plugin = UIPlugin.getDefault();
		IDialogSettings workbenchSettings = plugin.getDialogSettings();
		IDialogSettings section = workbenchSettings.getSection(EXPORT_DIALOG_SETTINGS);
		if (section == null) {
			section = workbenchSettings.addNewSection(EXPORT_DIALOG_SETTINGS);
		}
		setDialogSettings(section);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	@Override
	public void addPages() {
		IWizardPage page = new PeerExportWizardPage(fSelection);
		addPage(page);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		return ((PeerExportWizardPage)getStartingPage()).finish();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.fSelection = selection;
		setWindowTitle(Messages.PeerExportWizard_title);
		setNeedsProgressMonitor(true);
	}
}
