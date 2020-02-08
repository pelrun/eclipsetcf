/*******************************************************************************
* Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.editor.sections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistableNodeProperties;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.statushandler.StatusHandlerUtil;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.core.peers.Peer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.ui.editor.controls.InfoSectionPeerNameControl;
import org.eclipse.tcf.te.tcf.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.forms.parts.AbstractSection;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.tcf.te.ui.views.editor.pages.AbstractEditorPage;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Peer general information section implementation.
 */
public class GeneralInformationSection extends AbstractSection {
	// The section sub controls
	private InfoSectionPeerNameControl nameControl = null;

	// Reference to the original data object
	/* default */ IPeerNode od;
	// Reference to a copy of the original data
	/* default */ final IPropertiesContainer odc = new PropertiesContainer();
	// Reference to the properties container representing the working copy for the section
	/* default */ final IPropertiesContainer wc = new PropertiesContainer();

	// The list of existing configuration names. Used to generate a unique name
	// and validate the wizard
	/* default */ final java.util.List<String> usedNames = new ArrayList<String>();

	/**
	 * Constructor.
	 *
	 * @param form The parent managed form. Must not be <code>null</code>.
	 * @param parent The parent composite. Must not be <code>null</code>.
	 */
	public GeneralInformationSection(IManagedForm form, Composite parent) {
		super(form, parent, Section.DESCRIPTION);
		createClient(getSection(), form.getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	@Override
	public void dispose() {
		if (nameControl != null) { nameControl.dispose(); nameControl = null; }
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (InfoSectionPeerNameControl.class.equals(adapter)) {
			return nameControl;
		}
		return super.getAdapter(adapter);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		Assert.isNotNull(section);
		Assert.isNotNull(toolkit);

		// Configure the section
		section.setText(Messages.GeneralInformationSection_title);
		section.setDescription(Messages.GeneralInformationSection_description);

		if (section.getParent().getLayout() instanceof GridLayout) {
			section.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		// Create the section client
		Composite client = createClientContainer(section, 2, toolkit);
		Assert.isNotNull(client);
		section.setClient(client);

		// Create the peer name control
		nameControl = new InfoSectionPeerNameControl(this) {
			@Override
			public boolean isValid() {
				boolean valid = true;

				String name = getEditFieldControlTextForValidation();
				if (!"".equals(name)) { //$NON-NLS-1$
					// Name is not empty -> check against the list of used names
						valid = !infoSection.usedNames.contains(name.trim().toUpperCase());
						if (!valid) {
							setMessage(Messages.GeneralInformationSection_error_nameInUse, IMessageProvider.ERROR);
						}
				}

				if (!valid && getControlDecoration() != null) {
					// Setup and show the control decoration if necessary
					if (isEnabled()) {
						// Update the control decorator
						updateControlDecoration(getMessage(), getMessageType());
					}
				}

				return valid ? super.isValid() : false;
			}
		};
		nameControl.setFormToolkit(toolkit);
		nameControl.setParentControlIsInnerPanel(true);
		nameControl.setupPanel(client);

		// Adjust the control enablement
		updateEnablement();

		// Mark the control update as completed now
		setIsUpdating(false);
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
				if (node instanceof IPeerNode) {
					setupData((IPeerNode)node);
				}
			}
		} else {
			// Evaluate the dirty state even if going inactive
			dataChanged(null);
		}
	}

	/**
	 * Initialize the page widgets based of the data from the given peer node.
	 * <p>
	 * This method may called multiple times during the lifetime of the page and
	 * the given peer node might be even <code>null</code>.
	 *
	 * @param node The peer node or <code>null</code>.
	 */
	public void setupData(final IPeerNode node) {
		// If the section is dirty, nothing is changed
		if (isDirty()) return;

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
		if (node == null) return;

		// Thread access to the model is limited to the executors thread.
		// Copy the data over to the working copy to ease the access.
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				// The section is handling the ID, the name and
				// the link state. Ignore other properties.
				odc.setProperty(IPeer.ATTR_ID, node.getPeer().getAttributes().get(IPeer.ATTR_ID));
				odc.setProperty(IPeer.ATTR_NAME, node.getPeer().getAttributes().get(IPeer.ATTR_NAME));
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
			// Mark the control update as in-progress now
			setIsUpdating(true);

			if (nameControl != null) {
				nameControl.setEditFieldControlText(wc.getStringProperty(IPeer.ATTR_NAME));
			}

			// Mark the control update as completed now
			setIsUpdating(false);
		}

		initializeUsedNameList();

		// Re-evaluate the dirty state
		dataChanged(null);

		// Adjust the control enablement
		updateEnablement();
	}

	/**
	 * Stores the page widgets current values to the given peer node.
	 * <p>
	 * This method may called multiple times during the lifetime of the page and
	 * the given peer node might be even <code>null</code>.
	 *
	 * @param node The peer node or <code>null</code>.
	 */
	public void extractData(final IPeerNode node) {
		// If no data is available, we are done
		if (node == null) {
			return;
		}

		// Extract the widget data into the working copy
		if (nameControl != null) {
			String name = nameControl.getEditFieldControlText();
			boolean used = name != null && usedNames.contains(name.trim().toUpperCase());
			if (!used && name != null && !"".equals(name.trim()) ) { //$NON-NLS-1$
				wc.setProperty(IPeer.ATTR_NAME, name);
			} else {
				// Build up the message template
				String template = NLS.bind(Messages.OverviewEditorPage_error_save, wc.getStringProperty(IPeer.ATTR_NAME), Messages.PossibleCause);
				// Handle the status
				Status status = new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(), used ? Messages.GeneralInformationSection_error_nameInUse : Messages.GeneralInformationSection_error_emptyName);
				StatusHandlerUtil.handleStatus(status, od, template, null, IContextHelpIds.MESSAGE_SAVE_FAILED, GeneralInformationSection.this, null);
			}
		}

		// If the peer name changed, copy the working copy data back to
		// the original properties container
		if (!odc.getStringProperty(IPeer.ATTR_NAME).equals(wc.getStringProperty(IPeer.ATTR_NAME))) {
			Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					// To update the peer attributes, the peer needs to be recreated
					IPeer oldPeer = node.getPeer();
					// Create a write able copy of the peer attributes
					Map<String, String> attributes = new HashMap<String, String>(oldPeer.getAttributes());
					// Update the (managed) attributes from the working copy
					attributes.put(IPeer.ATTR_NAME, wc.getStringProperty(IPeer.ATTR_NAME));
					// Remove the persistence storage URI (if set)
					attributes.remove(IPersistableNodeProperties.PROPERTY_URI);
					// Create the new peer
					IPeer newPeer = new Peer(attributes);
					// Update the peer node instance (silently)
					boolean changed = node.setChangeEventsEnabled(false);
					node.setProperty(IPeerNodeProperties.PROPERTY_INSTANCE, newPeer);
					if (changed) node.setChangeEventsEnabled(true);
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#isValid()
	 */
	@Override
	public boolean isValid() {
	    // Validation is skipped while the controls are updated
	    if (isUpdating()) return true;

		boolean valid =  super.isValid();

		if (nameControl != null) {
			valid &= nameControl.isValid();
			setMessage(nameControl.getMessage(), nameControl.getMessageType());
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

		// Remember the old name
		String oldName = odc.getStringProperty(IPeer.ATTR_NAME);
		// Extract the data into the original data node
		extractData(od);

		// If the name changed, trigger a delete of the old data
		if (!oldName.equals(wc.getStringProperty(IPeer.ATTR_NAME))) {
			try {
				// Get the persistence service
				IURIPersistenceService uRIPersistenceService = ServiceManager.getInstance().getService(IURIPersistenceService.class);
				if (uRIPersistenceService == null) {
					throw new IOException("Persistence service instance unavailable."); //$NON-NLS-1$
				}
				// Remove the old persistence storage using the original data copy
				Map<String,String> oldData = new HashMap<String, String>();
				for (String key : odc.getProperties().keySet()) {
					oldData.put(key, odc.getStringProperty(key));
				}
				uRIPersistenceService.delete(new Peer(oldData), null);
			} catch (IOException e) {
				// Build up the message template
				String template = NLS.bind(Messages.GeneralInformationSection_error_delete, oldName, Messages.PossibleCause);
				// Handle the status
				StatusHandlerUtil.handleStatus(StatusHelper.getStatus(e), od, template, null, IContextHelpIds.MESSAGE_DELETE_FAILED, GeneralInformationSection.this, null);
			}
		}
	}

	/**
	 * Called to signal that the data associated has been changed.
	 *
	 * @param e The event which triggered the invocation or <code>null</code>.
	 */
	public void dataChanged(TypedEvent e) {
	    // dataChanged is not evaluated while the controls are updated
	    if (isUpdating()) return;

		boolean isDirty = false;

		// Compare the data
		if (nameControl != null) {
			String name = nameControl.getEditFieldControlText();
			if ("".equals(name)) { //$NON-NLS-1$
				String value = odc.getStringProperty(IPeer.ATTR_NAME);
				isDirty |= value != null && !"".equals(value.trim()); //$NON-NLS-1$
			} else {
				isDirty |= !odc.isProperty(IPeer.ATTR_NAME, name);
			}
		}

		// If dirty, mark the form part dirty.
		// Otherwise call refresh() to reset the dirty (and stale) flag
		markDirty(isDirty);
	}

	/**
	 * Updates the control enablement.
	 */
	protected void updateEnablement() {
		// Determine the input
		final Object input = getManagedForm().getInput();

		if (input instanceof IPeerNode) {
			SWTControlUtil.setEnabled(nameControl.getEditFieldControl(), ((IPeerNode)input).getConnectState() == IConnectable.STATE_DISCONNECTED);
		}
	}

	/**
	 * Initialize the used name list.
	 */
	protected void initializeUsedNameList() {
		usedNames.clear();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// Get all peer model objects
				IPeerNode[] peers = ModelManager.getPeerModel().getPeerNodes();
				// Loop them and find the ones which are of our handled types
				for (IPeerNode peerNode : peers) {
					if (!peerNode.equals(od)) {
						String name = peerNode.getPeer().getName();
						Assert.isNotNull(name);
						if (!"".equals(name) && !usedNames.contains(name)) { //$NON-NLS-1$
							usedNames.add(name.trim().toUpperCase());
						}
					}
				}
			}
		};

		Assert.isTrue(!Protocol.isDispatchThread());
		Protocol.invokeAndWait(runnable);
	}
}
