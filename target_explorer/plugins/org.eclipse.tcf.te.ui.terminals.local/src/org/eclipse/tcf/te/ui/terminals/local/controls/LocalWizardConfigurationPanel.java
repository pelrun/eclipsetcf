/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.local.controls;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tcf.te.ui.controls.BaseDialogPageControl;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode;
import org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Serial wizard configuration panel implementation.
 */
public class LocalWizardConfigurationPanel extends AbstractConfigurationPanel implements IDataExchangeNode {

	private IResource resource;

	/**
	 * Constructor.
	 *
	 * @param parentControl The parent control. Must not be <code>null</code>!
	 */
	public LocalWizardConfigurationPanel(BaseDialogPageControl parentControl) {
	    super(parentControl);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel#setupPanel(org.eclipse.swt.widgets.Composite, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	@Override
    public void setupPanel(Composite parent, FormToolkit toolkit) {
    	Composite panel = new Composite(parent, SWT.NONE);
    	panel.setLayout(new GridLayout());
    	panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    	Label label = new Label(panel, SWT.HORIZONTAL);
    	GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		layoutData.widthHint = 300;
		layoutData.heightHint = 100;
		label.setLayoutData(layoutData);

		resource = getSelectionResource();
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
    	// set the terminal connector id for serial
    	data.setProperty(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID, "org.eclipse.tcf.te.ui.terminals.local.LocalConnector"); //$NON-NLS-1$

    	// set the connector type for serial
    	data.setProperty(ITerminalsConnectorConstants.PROP_CONNECTOR_TYPE_ID, "org.eclipse.tcf.te.ui.terminals.type.local"); //$NON-NLS-1$

    	// if we have a IResource selection use the location for working dir
    	if (resource != null){
    		String dir = resource.getProject().getLocation().toString();
        	data.setProperty(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR, dir);
    	}
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
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel#doSaveWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
    public void doSaveWidgetValues(IDialogSettings settings, String idPrefix) {
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.panels.AbstractConfigurationPanel#doRestoreWidgetValues(org.eclipse.jface.dialogs.IDialogSettings, java.lang.String)
	 */
	@Override
    public void doRestoreWidgetValues(IDialogSettings settings, String idPrefix) {
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

	/**
	 * Returns the IResource from the current selection
	 *
	 * @return the IResource, or <code>null</code>.
	 */
	private IResource getSelectionResource() {
		ISelectionService selectionService = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
		ISelection selection = selectionService != null ? selectionService.getSelection() : StructuredSelection.EMPTY;

		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			if (element instanceof IResource){
				return ((IResource)element);
			}
			if (element instanceof IAdaptable) {
				return (IResource) ((IAdaptable) element).getAdapter(IResource.class);
			}
		}
		return null;
	}
}
