/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.terminals.ui.controls;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tcf.te.ui.controls.BaseDialogPageControl;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode;
import org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Terminals (TCF) wizard configuration panel implementation.
 */
public class TerminalsWizardConfigurationPanel extends AbstractConfigurationPanel implements IDataExchangeNode {

	/**
	 * Constructor.
	 *
	 * @param parentControl The parent control. Must not be <code>null</code>!
	 */
	public TerminalsWizardConfigurationPanel(BaseDialogPageControl parentControl) {
	    super(parentControl);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#setupPanel(org.eclipse.swt.widgets.Composite, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	@Override
	public void setupPanel(Composite parent, FormToolkit toolkit) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		panel.setLayoutData(data);

		// Create the host selection combo
		if (isWithoutSelection()) createHostsUI(panel, true);

		// Create the encoding selection combo
		createEncodingUI(panel, false);

		setControl(panel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#dataChanged(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer, org.eclipse.swt.events.TypedEvent)
	 */
	@Override
	public boolean dataChanged(IPropertiesContainer data, TypedEvent e) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.wizards.interfaces.ISharedDataWizardPage#setupData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
    public void setupData(IPropertiesContainer data) {
    }


	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.wizards.interfaces.ISharedDataWizardPage#extractData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
    public void extractData(IPropertiesContainer data) {
    	// set the terminal connector id for terminals (TCF)
    	data.setProperty(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID, "org.eclipse.tcf.te.tcf.terminals.ui.TerminalsConnector");

    	// set the connector type for terminals (TCF)
    	data.setProperty(ITerminalsConnectorConstants.PROP_CONNECTOR_TYPE_ID, "org.eclipse.tcf.te.ui.terminals.type.terminals");

    	// Extract the encoding
		data.setProperty(ITerminalsConnectorConstants.PROP_ENCODING, getEncoding());
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel#fillSettingsForHost(java.lang.String)
	 */
	@Override
	protected void fillSettingsForHost(String host){
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel#saveSettingsForHost(boolean)
	 */
	@Override
	protected void saveSettingsForHost(boolean add){
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.panels.AbstractWizardConfigurationPanel#isValid()
	 */
	@Override
    public boolean isValid(){
		return isEncodingValid();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel#doSaveWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
    public void doSaveWidgetValues(IDialogSettings settings, String idPrefix) {
		Assert.isNotNull(settings);
		String encoding = getEncoding();
		if (encoding != null) {
			settings.put(getParentControl().prefixDialogSettingsSlotId(ITerminalsConnectorConstants.PROP_ENCODING, idPrefix), encoding);
		}
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel#doRestoreWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
    public void doRestoreWidgetValues(IDialogSettings settings, String idPrefix) {
		Assert.isNotNull(settings);
		String encoding = settings.get(getParentControl().prefixDialogSettingsSlotId(ITerminalsConnectorConstants.PROP_ENCODING, idPrefix));
		if (encoding != null && encoding.trim().length() > 0) {
			setEncoding(encoding);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel#getHostFromSettings()
	 */
	@Override
    protected String getHostFromSettings() {
		return null;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel#isWithHostList()
	 */
	@Override
    public boolean isWithHostList() {
    	return false;
    }
}
