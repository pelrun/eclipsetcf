/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.sections;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.core.TransientPeer;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.interfaces.ISimulatorService;
import org.eclipse.tcf.te.tcf.core.peers.Peer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModelProperties;
import org.eclipse.tcf.te.tcf.locator.nodes.PeerRedirector;
import org.eclipse.tcf.te.tcf.locator.utils.SimulatorUtils;
import org.eclipse.tcf.te.tcf.ui.controls.SimulatorTypeSelectionControl;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.forms.parts.AbstractSection;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode;
import org.eclipse.tcf.te.ui.jface.interfaces.IValidatingContainer;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.tcf.te.ui.views.editor.pages.AbstractEditorPage;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Simulator section implementation.
 */
public class SimulatorTypeSelectionSection extends AbstractSection implements IDataExchangeNode {
	// The section sub controls
	/* default */ Button real;
	/* default */ SimulatorTypeSelectionControl type;

	// Reference to the original data object
	/* default */ IPeerModel od;
	// Reference to a copy of the original data
	/* default */ final IPropertiesContainer odc = new PropertiesContainer();
	// Reference to the properties container representing the working copy for the section
	/* default */ final IPropertiesContainer wc = new PropertiesContainer();

	protected static final int SELECTION_REAL = 0;
	protected static final int SELECTION_SIM = 1;

	/**
	 * Constructor.
	 *
	 * @param form The parent managed form. Must not be <code>null</code>.
	 * @param parent The parent composite. Must not be <code>null</code>.
	 */
	public SimulatorTypeSelectionSection(IManagedForm form, Composite parent) {
		super(form, parent, SWT.NONE);
		createClient(getSection(), form.getToolkit());
	}

	/**
	 * Constructor.
	 *
	 * @param form The parent managed form. Must not be <code>null</code>.
	 * @param parent The parent composite. Must not be <code>null</code>.
	 * @param style The section style.
	 */
	public SimulatorTypeSelectionSection(IManagedForm form, Composite parent, int style) {
		super(form, parent, style);
		createClient(getSection(), form.getToolkit());
	}

	/**
	 * Get the original data.
	 * @return The original set data or <code>null</code>;
	 */
	public Object getOriginalData() {
		return od;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	@Override
	public void dispose() {
		if (type != null) { type.dispose(); type = null; }
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#getValidatingContainer()
	 */
	@Override
	public IValidatingContainer getValidatingContainer() {
		Object container = getManagedForm().getContainer();
		return container instanceof IValidatingContainer ? (IValidatingContainer)container : null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		Assert.isNotNull(section);
		Assert.isNotNull(toolkit);

		// Configure the section
		section.setText(Messages.TargetSelectorSection_title);
		if (section.getParent().getLayout() instanceof GridLayout) {
			section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}

		// Create the section client
		Composite client = createClientContainer(section, 3, toolkit);
		Assert.isNotNull(client);
		section.setClient(client);

		real = toolkit.createButton(client, Messages.TargetSelectorSection_button_enableReal, SWT.RADIO);
		GridData gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		gd.horizontalSpan = 3;
		real.setSelection(true);
		real.setLayoutData(gd);
		real.setBackground(client.getBackground());
		real.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (real.getSelection()) {
					onSelectionChanged(SELECTION_REAL);
				}
				// validate the page
				getValidatingContainer().validate();
				dataChanged(null);
			}
		});

		type = new SimulatorTypeSelectionControl(this) {
			@SuppressWarnings("synthetic-access")
			@Override
			protected void onLabelControlSelectedChanged() {
				super.onLabelControlSelectedChanged();
				if (type.isLabelControlSelected()) {
					onSelectionChanged(SELECTION_SIM);
					if (!isUpdating()) {
						onSimulatorChanged(false, true, getSelectedSimulatorId(), getSelectedSimulatorId(), getSimulatorConfig(), getSimulatorConfig());
					}
				}
			}
			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				String newId = getSelectedSimulatorId();
				onSimulatorChanged(isLabelControlSelected(), isLabelControlSelected(), null, newId, getSimulatorConfig(), getSimulatorConfig());
			}
			@Override
			protected void onButtonControlSelected() {
				String oldConfig = getSimulatorConfig();
				super.onButtonControlSelected();
				String newConfig = getSimulatorConfig();
				if ((newConfig != null && !newConfig.equals(oldConfig)) || (newConfig == null && oldConfig != null)) {
					onSimulatorChanged(isLabelControlSelected(), isLabelControlSelected(), getSelectedSimulatorId(), getSelectedSimulatorId(), oldConfig, newConfig);
				}
			}
		};
		type.setLabelIsButton(true);
		type.setLabelButtonStyle(SWT.RADIO);
		type.setEditFieldLabel(Messages.TargetSelectorSection_button_enableSimulator);
		type.setParentControlIsInnerPanel(true);
		type.setupPanel(client);

		// Adjust the control enablement
		updateEnablement();

		// Mark the control update as completed now
		setIsUpdating(false);
	}

	/**
	 * Called on radio button selection changed.
	 * @param selectionType The new selected type.
	 */
	protected void onSelectionChanged(int selectionType) {
	}

	/**
	 * Called on simulator enabled, simulator type or simulator configuration changed.
	 *
	 * @param oldEnabled The old simulator enabled state.
	 * @param newEnabled The simulator enabled state.
	 * @param oldType The old selected simulator type.
	 * @param newType The selected simulator type.
	 * @param oldConfig The old simulator configuration.
	 * @param newConfig The new simulator configuration.
	 */
	protected void onSimulatorChanged(boolean oldEnabled, boolean newEnabled, String oldType, String newType, String oldConfig, String newConfig) {
	}

	/**
	 * Indicates whether the sections parent page has become the active in the editor.
	 *
	 * @param active <code>True</code> if the parent page should be visible, <code>false</code> otherwise.
	 */
	public void setActive(boolean active) {
		// If the parent page has become the active and it does not contain
		// unsaved data, than fill in the data from the selected node
		if (active) {
			// Leave everything unchanged if the page is in dirty state
			if (getManagedForm().getContainer() instanceof AbstractEditorPage
							&& !((AbstractEditorPage)getManagedForm().getContainer()).isDirty()) {
				Object node = ((AbstractEditorPage)getManagedForm().getContainer()).getEditorInputNode();
				if (node instanceof IPeerModel) {
					setupData((IPeerModel)node);
				}
			}
		} else {
			// Evaluate the dirty state even if going inactive
			dataChanged(null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#setupData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void setupData(IPropertiesContainer data) {
		// Mark the control update as in-progress now
		setIsUpdating(true);

		// Initialize the simulator type selection control
		if (type != null) {
			type.initialize(od);
			type.setSelectedSimulatorId(data.getStringProperty(IPeerModelProperties.PROP_SIM_TYPE));
			type.setSimulatorConfig(data.getStringProperty(IPeerModelProperties.PROP_SIM_PROPERTIES));
			type.setLabelControlSelection(data.getBooleanProperty(IPeerModelProperties.PROP_SIM_ENABLED));
		}

		SWTControlUtil.setSelection(real, !data.getBooleanProperty(IPeerModelProperties.PROP_SIM_ENABLED));

		onSelectionChanged(data.getBooleanProperty(IPeerModelProperties.PROP_SIM_ENABLED) ? SELECTION_SIM : SELECTION_REAL);

		// Mark the control update as completed now
		setIsUpdating(false);
		// Re-evaluate the dirty state
		dataChanged(null);
	}

	/**
	 * Initialize the page widgets based of the data from the given peer model
	 * node.
	 * <p>
	 * This method may called multiple times during the lifetime of the page and
	 * the given context node might be even <code>null</code>.
	 *
	 * @param node The peer model node or <code>null</code>.
	 */
	public void setupData(final IPeerModel node) {
		// If the section is dirty, nothing is changed
		if (isDirty()) {
			return;
		}

		boolean updateWidgets = true;

		// If the passed in node is the same as the previous one,
		// no need for updating the section widgets.
		if ((node == null && od == null) || (node != null && node.equals(od))) {
			updateWidgets = false;
		}

		// Besides the node itself, we need to look at the node data to determine
		// if the widgets needs to be updated. For the comparisation, keep the
		// current properties of the original data copy in a temporary container.
		final IPropertiesContainer previousOdc = new PropertiesContainer();
		previousOdc.setProperties(odc.getProperties());

		// Store a reference to the original data
		od = node;
		// Clean the original data copy
		odc.clearProperties();
		// Clean the working copy
		wc.clearProperties();

		// If no data is available, we are done
		if (node == null) {
			return;
		}

		// Thread access to the model is limited to the executors thread.
		// Copy the data over to the working copy to ease the access.
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				// The section is handling the simulator related properties
				// Ignore other properties.
				odc.setProperty(IPeerModelProperties.PROP_SIM_ENABLED, node.getPeer().getAttributes().get(IPeerModelProperties.PROP_SIM_ENABLED));
				odc.setProperty(IPeerModelProperties.PROP_SIM_PROPERTIES, node.getPeer().getAttributes().get(IPeerModelProperties.PROP_SIM_PROPERTIES));
				odc.setProperty(IPeerModelProperties.PROP_SIM_TYPE, node.getPeer().getAttributes().get(IPeerModelProperties.PROP_SIM_TYPE));
				// Initially, the working copy is a duplicate of the original data copy
				wc.setProperties(odc.getProperties());
			}
		});

		// From here on, work with the working copy only!

		// If the original data copy does not match the previous original
		// data copy, the widgets needs to be updated to present the correct data.
		if (!previousOdc.getProperties().equals(odc.getProperties())) {
			updateWidgets = true;
		}

		if (updateWidgets) {
			setupData(wc);
		}
		else {
			// Re-evaluate the dirty state
			dataChanged(null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#extractData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void extractData(IPropertiesContainer data) {
		Assert.isNotNull(data);

		// Extract the widget data into the working copy
		if (type != null) {
			data.setProperty(IPeerModelProperties.PROP_SIM_ENABLED, type.isLabelControlSelected());
			data.setProperty(IPeerModelProperties.PROP_SIM_TYPE, type.getSelectedSimulatorId());
			data.setProperty(IPeerModelProperties.PROP_SIM_PROPERTIES, type.getSimulatorConfig());
		}
	}

	/**
	 * Stores the page widgets current values to the given peer node.
	 * <p>
	 * This method may called multiple times during the lifetime of the page and
	 * the given peer node might be even <code>null</code>.
	 *
	 * @param node The peer node or <code>null</code>.
	 */
	public void extractData(final IPeerModel node) {
		// If no data is available, we are done
		if (node == null) {
			return;
		}

		// Extract the widget data into the working copy
		extractData(wc);

		// If the data has not changed compared to the original data copy,
		// we are done here and return immediately
		if (odc.equals(wc)) {
			return;
		}

		// Copy the working copy data back to the original properties container
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				boolean isSimEnabled = wc.getBooleanProperty(IPeerModelProperties.PROP_SIM_ENABLED);
				String configs = wc.getStringProperty(IPeerModelProperties.PROP_SIM_PROPERTIES);
				String type = wc.getStringProperty(IPeerModelProperties.PROP_SIM_TYPE);

				// To update the peer attributes, the peer needs to be recreated
				IPeer oldPeer = node.getPeer();
				// Create a write able copy of the peer attributes
				Map<String, String> attributes = new HashMap<String, String>(oldPeer.getAttributes());
				// Update the data
				if (isSimEnabled) {
					attributes.put(IPeerModelProperties.PROP_SIM_ENABLED, Boolean.toString(isSimEnabled));
				} else {
					attributes.remove(IPeerModelProperties.PROP_SIM_ENABLED);
				}
				if (configs != null) {
					attributes.put(IPeerModelProperties.PROP_SIM_PROPERTIES, configs);
				} else {
					attributes.remove(IPeerModelProperties.PROP_SIM_PROPERTIES);
				}
				if (type != null) {
					attributes.put(IPeerModelProperties.PROP_SIM_TYPE, type);
				} else {
					attributes.remove(IPeerModelProperties.PROP_SIM_TYPE);
				}
				// And merge it to the peer model node
				if (oldPeer instanceof TransientPeer && !(oldPeer instanceof PeerRedirector || oldPeer instanceof Peer)) {
					// Create a peer object
					IPeer newPeer = new Peer(attributes);
					// Update the peer instance
					node.setProperty(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModelProperties.PROP_INSTANCE, newPeer);
				} else {
					if (oldPeer instanceof PeerRedirector) {
						((PeerRedirector)oldPeer).updateAttributes(attributes);
					} else if (oldPeer instanceof Peer) {
						((Peer)oldPeer).updateAttributes(attributes);
					}
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#isValid()
	 */
	@Override
	public boolean isValid() {
		// Validation is skipped while the controls are updated
		if (isUpdating()) {
			return true;
		}

		boolean valid = super.isValid();

		if (type != null && type.isLabelControlSelected()) {
			valid &= type.isValid();
			if (type.getMessageType() > getMessageType()) {
				setMessage(type.getMessage(), type.getMessageType());
			}
		}

		return valid;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	@Override
	public void commit(boolean onSave) {
		// Remember the current dirty state
		boolean needsSaving = isDirty();
		// Call the super implementation (resets the dirty state)
		super.commit(onSave);

		// Nothing to do if not on save or saving is not needed
		if (!onSave || !needsSaving) {
			return;
		}

		// Extract the data into the peer model node
		extractData((IPeerModel)getManagedForm().getInput());
	}

	/**
	 * Called to signal that the data associated has been changed.
	 *
	 * @param e The event which triggered the invocation or <code>null</code>.
	 */
	public void dataChanged(TypedEvent e) {
		// dataChanged is not evaluated while the controls are updated
		if (isUpdating()) {
			return;
		}

		boolean isDirty = false;

		// Compare the data
		if (type != null) {
			boolean oldEnabled = odc.getBooleanProperty(IPeerModelProperties.PROP_SIM_ENABLED);
			isDirty |= (oldEnabled != type.isLabelControlSelected());

			if (type.isLabelControlSelected()) {
				String newType = type.getSelectedSimulatorId();
				String oldType = odc.getStringProperty(IPeerModelProperties.PROP_SIM_TYPE);
				if (newType == null || "".equals(newType)) { //$NON-NLS-1$
					isDirty |= oldType != null && !"".equals(oldType); //$NON-NLS-1$
				} else {
					isDirty |= !newType.equals(oldType);
				}

				String newConfig = type.getSimulatorConfig();
				String oldConfig = odc.getStringProperty(IPeerModelProperties.PROP_SIM_PROPERTIES);
				if (newConfig == null || "".equals(newConfig)) { //$NON-NLS-1$
					isDirty |= oldConfig != null && !"".equals(oldConfig); //$NON-NLS-1$
				}
				else {
					isDirty |= !newConfig.equals(oldConfig);
				}
			}
		}

		// If dirty, mark the form part dirty.
		// Otherwise call refresh() to reset the dirty (and stale) flag
		markDirty(isDirty);

		// Adjust the control enablement
		updateEnablement();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (SimulatorTypeSelectionControl.class.equals(adapter)) {
			return type;
		}
		return super.getAdapter(adapter);
	}

	/**
	 * Updates the control enablement.
	 */
	protected void updateEnablement() {
		SimulatorUtils.Result simulator = od != null ? SimulatorUtils.getSimulatorService(od) : null;

		boolean enabled = simulator == null || simulator.service.getState(od, simulator.settings) == ISimulatorService.State.Stopped;
		SWTControlUtil.setEnabled(real, enabled);

		if (type != null) {
			SWTControlUtil.setEnabled(type.getEditFieldControl(), type.isLabelControlSelected() && enabled);
		}
	}
}
