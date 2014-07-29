/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.internal.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.ui.controls.BaseDialogPageControl;
import org.eclipse.tcf.te.ui.jface.dialogs.CustomTrayDialog;
import org.eclipse.tcf.te.ui.terminals.help.IContextHelpIds;
import org.eclipse.tcf.te.ui.terminals.nls.Messages;
import org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Encoding selection dialog implementation.
 */
public class EncodingSelectionDialog extends CustomTrayDialog {
	// The selected encoding or null
	/* default */ String encoding = null;

	// Reference to the encodings panel
	private EncodingPanel encodingPanel = null;

	/**
	 * Encodings panel implementation
	 */
	protected class EncodingPanel extends AbstractConfigurationPanel {

		/**
		 * Constructor
		 */
        public EncodingPanel() {
	        super(new BaseDialogPageControl());
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

    		// Create the encoding selection combo
    		createEncodingUI(panel, false);
    		if (EncodingSelectionDialog.this.encoding != null) {
    			setEncoding(EncodingSelectionDialog.this.encoding);
    		}

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
		 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel#saveSettingsForHost(boolean)
		 */
        @Override
        protected void saveSettingsForHost(boolean add) {
        }

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel#fillSettingsForHost(java.lang.String)
		 */
        @Override
        protected void fillSettingsForHost(String host) {
        }

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel#getHostFromSettings()
		 */
        @Override
        protected String getHostFromSettings() {
	        return null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel#getEncoding()
         */
        @Override
        public String getEncoding() {
            return super.getEncoding();
        }

        /* (non-Javadoc)
         * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel#setEncoding(java.lang.String)
         */
        @Override
        public void setEncoding(String encoding) {
            super.setEncoding(encoding);
        }
	}

	/**
     * Constructor.
     *
	 * @param shell The parent shell or <code>null</code>.
     */
    public EncodingSelectionDialog(Shell shell) {
	    super(shell, IContextHelpIds.ENCODING_SELECTION_DIALOG);
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTrayDialog#createDialogAreaContent(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void createDialogAreaContent(Composite parent) {
        super.createDialogAreaContent(parent);

    	setDialogTitle(Messages.EncodingSelectionDialog_title);

        Composite panel = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0; layout.marginWidth = 0;
        panel.setLayout(layout);
        panel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

        encodingPanel = new EncodingPanel();
        encodingPanel.setupPanel(panel, null);

        applyDialogFont(panel);
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTrayDialog#okPressed()
     */
    @Override
    protected void okPressed() {
    	// Save the selected encoding
    	if (encodingPanel != null) encoding = encodingPanel.getEncoding();
        super.okPressed();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#cancelPressed()
     */
    @Override
    protected void cancelPressed() {
    	// Reset the encoding
    	encoding = null;
        super.cancelPressed();
    }

    /**
     * Set the encoding to default to on creating the dialog.
     */
    public final void setEncoding(String encoding) {
    	this.encoding = encoding;
    }

    /**
     * Returns the selected encoding or <code>null</code>.
     */
    public final String getEncoding() {
    	return encoding;
    }
}
