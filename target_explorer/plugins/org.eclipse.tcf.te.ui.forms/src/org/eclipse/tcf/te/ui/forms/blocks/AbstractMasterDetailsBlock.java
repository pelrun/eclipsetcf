/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.forms.blocks;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.runtime.interfaces.IDisposable;
import org.eclipse.tcf.te.ui.forms.FormLayoutFactory;
import org.eclipse.tcf.te.ui.forms.parts.AbstractTreeSection;
import org.eclipse.tcf.te.ui.jface.interfaces.IValidatable;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;

/**
 * Abstract master details block implementation.
 */
public abstract class AbstractMasterDetailsBlock extends MasterDetailsBlock implements IDisposable, IAdaptable, IValidatable {

	private String message = null;
	private int type = -1;

	protected AbstractTreeSection treeSection;
	protected Color parentBackground = null;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IDisposable#dispose()
	 */
	@Override
	public void dispose() {
		if (treeSection != null) { treeSection.dispose(); treeSection = null; }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.MasterDetailsBlock#createContent(org.eclipse.ui.forms.IManagedForm, org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createContent(IManagedForm managedForm, Composite parent) {
		parentBackground = parent.getBackground();
		super.createContent(managedForm, parent);
		parent.setLayout(FormLayoutFactory.createFormGridLayout(false, 1));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.MasterDetailsBlock#createMasterPart(org.eclipse.ui.forms.IManagedForm, org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createMasterPart(IManagedForm managedForm, Composite parent) {
		Assert.isNotNull(managedForm);
		Assert.isNotNull(parent);

		// Adjust the background if necessary
		if (!parent.getBackground().equals(parentBackground)) {
			parent.setBackground(parentBackground);
		}

		// Create a container composite for the master section part
		Composite container = managedForm.getToolkit().createComposite(parent);
		container.setLayout(FormLayoutFactory.createMasterGridLayout(false, 1));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Setup the systems tree section part
		doSetupTreeSectionPart(managedForm, container);
	}

	/**
	 * Setup the GDB remote configurations tree section part.
	 *
	 * @param managedForm The managed form. Must not be <code>null</code>.
	 * @param parent The parent composite. Must not be <code>null</code>.
	 */
	protected void doSetupTreeSectionPart(IManagedForm managedForm, Composite parent) {
		Assert.isNotNull(managedForm);
		Assert.isNotNull(parent);

		treeSection = doCreateTreeSection(managedForm, parent);
		Assert.isNotNull(treeSection);
		treeSection.getSection().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		managedForm.addPart(treeSection);
	}

	/**
	 * Creates the tree section.
	 *
	 * @param managedForm The managed form. Must not be <code>null</code>.
	 * @param parent The parent composite. Must not be <code>null</code>.
	 *
	 * @return The tree section.
	 */
	protected abstract AbstractTreeSection doCreateTreeSection(IManagedForm managedForm, Composite parent);

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.MasterDetailsBlock#createToolBarActions(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	protected void createToolBarActions(IManagedForm managedForm) {
	}

	/**
	 * Indicates whether the sections parent page has become the active in the editor.
	 *
	 * @param active <code>True</code> if the parent page should be visible, <code>false</code> otherwise.
	 */
	public void setActive(boolean active) {
		if (treeSection != null) {
			treeSection.setActive(active);
			if (detailsPart.getCurrentPage() != null) {
				detailsPart.getCurrentPage().selectionChanged(treeSection, treeSection.getViewer().getSelection());
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.interfaces.IValidatable#isValid()
	 */
	@Override
	public boolean isValid() {
		boolean valid = true;

		if (treeSection != null) {
			valid &= treeSection.isValid();
			setMessage(treeSection.getMessage(), treeSection.getMessageType());
		}

		IDetailsPage detailsPage = detailsPart != null ? detailsPart.getCurrentPage() : null;
		if (detailsPage instanceof IValidatable) {
			IValidatable detailsSection = (IValidatable)detailsPage;
			valid &= detailsSection.isValid();
			if (getMessageType() < detailsSection.getMessageType()) {
				setMessage(detailsSection.getMessage(), detailsSection.getMessageType());
			}
		}

		return valid;
	}

	/**
	 * Set the message.
	 * @param message The message.
	 * @param type The message type.
	 */
	protected void setMessage(String message, int type) {
		this.message = message;
		this.type = type;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IMessageProvider#getMessage()
	 */
	@Override
	public String getMessage() {
		return message;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IMessageProvider#getMessageType()
	 */
	@Override
	public int getMessageType() {
		return type;
	}
}
