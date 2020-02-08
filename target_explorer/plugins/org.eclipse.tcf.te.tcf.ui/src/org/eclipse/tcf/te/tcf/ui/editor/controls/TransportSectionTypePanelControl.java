/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.editor.controls;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.tcf.te.tcf.ui.controls.TransportTypePanelControl;
import org.eclipse.tcf.te.tcf.ui.editor.sections.TransportSection;
import org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel;
import org.eclipse.tcf.te.ui.jface.interfaces.IValidatingContainer;

/**
 * Transport section transport simulator panel control implementation.
 */
public class TransportSectionTypePanelControl extends TransportTypePanelControl implements ModifyListener {
	// Reference to the parent transport section
	private final TransportSection transportSection;

	/**
	 * Constructor.
	 *
	 * @param transportSection The parent transport section. Must not be <code>null</code>.
	 */
	public TransportSectionTypePanelControl(TransportSection transportSection) {
		super(null);

		Assert.isNotNull(transportSection);
		this.transportSection = transportSection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.BaseControl#isValid()
	 */
	@Override
	public boolean isValid() {
		boolean valid = super.isValid();
		if (!valid) return false;

		TransportSectionTypeControl transportTypeControl = (TransportSectionTypeControl)transportSection.getAdapter(TransportSectionTypeControl.class);
		if (transportTypeControl != null) {
			// Get the currently selected transport simulator
			String transportType = transportTypeControl.getSelectedTransportType();
			if (transportType != null) {
				// get the panel for the transport simulator and validate the panel
				IWizardConfigurationPanel panel = getConfigurationPanel(transportType);
				// getConfigurationPanel(...) always return a non-null value
				Assert.isNotNull(panel);
				valid = panel.isValid();
				setMessage(panel.getMessage(), panel.getMessageType());
			}
		}

		return valid;
	}

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.ui.controls.BaseDialogPageControl#getValidatingContainer()
     */
    @Override
    public IValidatingContainer getValidatingContainer() {
		Object container = transportSection.getManagedForm().getContainer();
		return container instanceof IValidatingContainer ? (IValidatingContainer)container : null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
	 */
	@Override
    public void modifyText(ModifyEvent e) {
		transportSection.dataChanged(e);
	}
}
