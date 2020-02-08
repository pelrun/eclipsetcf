/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.terminals.ui.controls;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tm.terminal.view.ui.interfaces.IConfigurationPanelContainer;
import org.eclipse.tm.terminal.view.ui.panels.AbstractExtendedConfigurationPanel;

/**
 * Terminals (TCF) terminal launcher configuration panel implementation.
 */
public class TerminalsConfigurationPanel extends AbstractExtendedConfigurationPanel {

	/**
	 * Constructor.
	 *
	 * @param container The configuration panel container or <code>null</code>.
	 */
	public TerminalsConfigurationPanel(IConfigurationPanelContainer container) {
	    super(container);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.IConfigurationPanel#setupPanel(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void setupPanel(Composite parent) {
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
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel#extractData(java.util.Map)
	 */
	@Override
	public void extractData(Map<String, Object> data) {
		if (data == null) return;

    	// set the terminal connector id for terminals (TCF)
    	data.put(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID, "org.eclipse.tcf.te.tcf.terminals.ui.TerminalsConnector"); //$NON-NLS-1$

    	// Extract the encoding
		data.put(ITerminalsConnectorConstants.PROP_ENCODING, getEncoding());
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractExtendedConfigurationPanel#fillSettingsForHost(java.lang.String)
	 */
	@Override
	protected void fillSettingsForHost(String host){
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractExtendedConfigurationPanel#saveSettingsForHost(boolean)
	 */
	@Override
	protected void saveSettingsForHost(boolean add){
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel#isValid()
	 */
	@Override
    public boolean isValid(){
		return isEncodingValid();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractExtendedConfigurationPanel#doSaveWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
    public void doSaveWidgetValues(IDialogSettings settings, String idPrefix) {
		Assert.isNotNull(settings);
		// Save the encodings widget values
		doSaveEncodingsWidgetValues(settings, idPrefix);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractExtendedConfigurationPanel#doRestoreWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
    public void doRestoreWidgetValues(IDialogSettings settings, String idPrefix) {
		Assert.isNotNull(settings);
		// Restore the encodings widget values
		doRestoreEncodingsWidgetValues(settings, idPrefix);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractExtendedConfigurationPanel#getHostFromSettings()
	 */
	@Override
    protected String getHostFromSettings() {
		return null;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractExtendedConfigurationPanel#isWithHostList()
	 */
	@Override
    public boolean isWithHostList() {
    	return false;
    }
}
