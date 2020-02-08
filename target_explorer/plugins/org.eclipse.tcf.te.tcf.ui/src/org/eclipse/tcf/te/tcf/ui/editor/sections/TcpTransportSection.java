/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.editor.sections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.TypedEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.core.interfaces.ITransportTypes;
import org.eclipse.tcf.te.tcf.core.peers.Peer;
import org.eclipse.tcf.te.tcf.core.util.persistence.PeerDataHelper;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl;
import org.eclipse.tcf.te.ui.controls.net.RemoteHostAddressControl;
import org.eclipse.tcf.te.ui.controls.net.RemoteHostPortControl;
import org.eclipse.tcf.te.ui.controls.validator.NameOrIPValidator;
import org.eclipse.tcf.te.ui.controls.validator.Validator;
import org.eclipse.tcf.te.ui.controls.wire.network.NetworkAddressControl;
import org.eclipse.tcf.te.ui.controls.wire.network.NetworkCablePanel;
import org.eclipse.tcf.te.ui.forms.parts.AbstractSection;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode;
import org.eclipse.tcf.te.ui.jface.interfaces.IValidatingContainer;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.tcf.te.ui.views.editor.pages.AbstractEditorPage;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Transport section providing TCP transport only.
 */
public class TcpTransportSection extends AbstractSection implements IDataExchangeNode {
	private BaseEditBrowseTextControl proxyControl = null;
	private MyRemoteHostAddressControl addressControl = null;
	private MyRemoteHostPortControl portControl = null;

	private boolean isAutoPort = false;
	private String proxies = null;

	// Reference to the original data object
	protected IPeerNode od;
	// Reference to a copy of the original data
	/* default */final IPropertiesContainer odc = new PropertiesContainer();
	// Reference to the properties container representing the working copy for the section
	/* default */final IPropertiesContainer wc = new PropertiesContainer();

	/**
	 * Local address control implementation.
	 */
	protected class MyNetworkAddressControl extends NetworkAddressControl {


		/**
		 * Constructor.
		 *
		 * @param networkPanel The parent network cable. Must not be <code>null</code>.
		 */
		public MyNetworkAddressControl(NetworkCablePanel networkPanel) {
			super(networkPanel);
			setEditFieldLabel(Messages.MyNetworkAddressControl_label);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.controls.net.RemoteHostAddressControl#configureEditFieldValidator(org.eclipse.tcf.te.ui.controls.validator.Validator)
		 */
		@Override
		protected void configureEditFieldValidator(Validator validator) {
			if (validator instanceof NameOrIPValidator) {
				validator.setMessageText(NameOrIPValidator.INFO_MISSING_NAME_OR_IP, Messages.MyNetworkAddressControl_information_missingTargetNameAddress);
				validator.setMessageText(NameOrIPValidator.ERROR_INVALID_NAME_OR_IP, Messages.MyNetworkAddressControl_error_invalidTargetNameAddress);
				validator.setMessageText(NameOrIPValidator.ERROR_INVALID_NAME, Messages.MyNetworkAddressControl_error_invalidTargetNameAddress);
				validator.setMessageText(NameOrIPValidator.ERROR_INVALID_IP, Messages.MyNetworkAddressControl_error_invalidTargetIpAddress);
				validator.setMessageText(NameOrIPValidator.INFO_CHECK_NAME, getUserInformationTextCheckNameAddress());
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.controls.net.RemoteHostAddressControl#getUserInformationTextCheckNameAddress()
		 */
		@Override
		protected String getUserInformationTextCheckNameAddress() {
			return Messages.MyNetworkAddressControl_information_checkNameAddressUserInformation;
		}

        /* (non-Javadoc)
         * @see org.eclipse.tcf.te.ui.controls.BaseDialogPageControl#getValidatingContainer()
         */
        @Override
        public IValidatingContainer getValidatingContainer() {
    		Object container = TcpTransportSection.this.getManagedForm().getContainer();
    		return container instanceof IValidatingContainer ? (IValidatingContainer)container : null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#modifyText(org.eclipse.swt.events.ModifyEvent)
		 */
		@Override
		public void modifyText(ModifyEvent e) {
			super.modifyText(e);
			TcpTransportSection.this.dataChanged(e);
		}
	}

	/**
	 * Local remote host address control implementation.
	 */
	protected class MyRemoteHostAddressControl extends RemoteHostAddressControl {

		/**
		 * Constructor.
		 */
		public MyRemoteHostAddressControl() {
			super(null);

			setEditFieldLabel(Messages.MyRemoteHostAddressControl_label);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.controls.BaseDialogPageControl#getValidatingContainer()
		 */
		@Override
		public IValidatingContainer getValidatingContainer() {
    		Object container = TcpTransportSection.this.getManagedForm().getContainer();
    		return container instanceof IValidatingContainer ? (IValidatingContainer)container : null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#modifyText(org.eclipse.swt.events.ModifyEvent)
		 */
		@Override
		public void modifyText(ModifyEvent e) {
			super.modifyText(e);
			TcpTransportSection.this.dataChanged(e);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.controls.net.RemoteHostAddressControl#onButtonControlSelected()
		 */
		@Override
		protected void onButtonControlSelected() {
		    super.onButtonControlSelected();
			getValidatingContainer().setMessage(getMessage(), getMessageType());
		}
	}

	/**
	 * Local remote host port control implementation.
	 */
	public class MyRemoteHostPortControl extends RemoteHostPortControl {

		/**
		 * Constructor.
		 */
		public MyRemoteHostPortControl() {
			super(null);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.controls.BaseDialogPageControl#getValidatingContainer()
		 */
		@Override
		public IValidatingContainer getValidatingContainer() {
    		Object container = TcpTransportSection.this.getManagedForm().getContainer();
    		return container instanceof IValidatingContainer ? (IValidatingContainer)container : null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#modifyText(org.eclipse.swt.events.ModifyEvent)
		 */
		@Override
		public void modifyText(ModifyEvent e) {
			super.modifyText(e);
			TcpTransportSection.this.dataChanged(e);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param form The parent managed form. Must not be <code>null</code>.
	 * @param parent The parent composite. Must not be <code>null</code>.
	 */
	public TcpTransportSection(IManagedForm form, Composite parent) {
		super(form, parent, ExpandableComposite.EXPANDED);
		createClient(getSection(), form.getToolkit());
	}

	/**
	 * Constructor.
	 *
	 * @param form The parent managed form. Must not be <code>null</code>.
	 * @param parent The parent composite. Must not be <code>null</code>.
	 */
	public TcpTransportSection(IManagedForm form, Composite parent, boolean showTitleBar) {
		super(form, parent, showTitleBar ? ExpandableComposite.EXPANDED : ExpandableComposite.NO_TITLE, showTitleBar);
		createClient(getSection(), form.getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	@Override
	public void dispose() {
		if (proxyControl != null) { proxyControl.dispose(); proxyControl = null; }
		if (addressControl != null) { addressControl.dispose(); addressControl = null; }
		if (portControl != null) { portControl.dispose(); portControl = null; }
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		if (MyRemoteHostAddressControl.class.equals(adapter)) {
			return addressControl;
		}
		if (MyRemoteHostPortControl.class.equals(adapter)) {
			return portControl;
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
		section.setText(Messages.TcpTransportSection_title);

		if (section.getParent().getLayout() instanceof GridLayout) {
			section.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}

		// Create the section client
		Composite client = createClientContainer(section, 2, toolkit);
		Assert.isNotNull(client);
		section.setClient(client);

		proxyControl = new BaseEditBrowseTextControl(null);
		proxyControl.setFormToolkit(toolkit);
		proxyControl.setParentControlIsInnerPanel(true);
		proxyControl.setEditFieldLabel(Messages.TcpTransportSection_proxies_label);
		proxyControl.setReadOnly(true);
		proxyControl.setHasHistory(false);
		proxyControl.setHideBrowseButton(true);
		proxyControl.setupPanel(client);
		SWTControlUtil.setEnabled(proxyControl.getEditFieldControl(), false);

		addressControl = new MyRemoteHostAddressControl();
		addressControl.setFormToolkit(toolkit);
		addressControl.setupPanel(client);
		addressControl.getEditFieldControl().setFocus();

		portControl = new MyRemoteHostPortControl();
		portControl.setFormToolkit(toolkit);
		portControl.setParentControlIsInnerPanel(true);
		portControl.setupPanel(addressControl.getInnerPanelComposite());
		portControl.setEditFieldControlText(getDefaultPort());
	}

	/**
	 * Returns the default port to set to the port control.
	 *
	 * @return The default port to set or <code>null</code>.
	 */
	protected String getDefaultPort() {
		return "1534"; //$NON-NLS-1$
	}

	/**
	 * Set the auto port state.
	 *
	 * @param value <code>True</code> if the port is an "auto port", <code>false</code> otherwise.
	 */
	protected final void setIsAutoPort(boolean value) {
		isAutoPort = value;
		if (portControl != null) portControl.setEnabled(!isAutoPort);
	}

	/**
	 * Returns the auto port state.
	 *
	 * @return <code>True</code> if the port is an "auto port", <code>false</code> otherwise.
	 */
	protected final boolean isAutoPort() {
		return isAutoPort;
	}

	/**
	 * Indicates whether the sections parent page has become the active in the editor.
	 *
	 * @param active <code>True</code> if the parent page should be visible, <code>false</code>
	 *            otherwise.
	 */
	public void setActive(boolean active) {
		// If the parent page has become the active and it does not contain
		// unsaved data, than fill in the data from the selected node
		if (active) {
			// Leave everything unchanged if the page is in dirty state
			if (getManagedForm().getContainer() instanceof AbstractEditorPage && !((AbstractEditorPage) getManagedForm().getContainer()).isDirty()) {
				Object node = ((AbstractEditorPage) getManagedForm().getContainer()).getEditorInputNode();
				if (node instanceof IPeerNode) {
					setupData((IPeerNode) node);
				}
			}
		}
		else {
			// Evaluate the dirty state even if going inactive
			dataChanged(null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#setupData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
    @Override
    public void setupData(IPropertiesContainer data) {
		Assert.isNotNull(data);

		// Mark the control update as in-progress now
		setIsUpdating(true);

		boolean isAutoPort = data.getBooleanProperty(IPeerProperties.PROP_IP_PORT_IS_AUTO);

		if (proxyControl != null) {
			proxies = data.getStringProperty(IPeerProperties.PROP_PROXIES);
			IPeer[] proxyPeers = PeerDataHelper.decodePeerList(proxies);
			String proxyInfo = ""; //$NON-NLS-1$
			for (final IPeer proxy : proxyPeers) {
				final AtomicReference<ILocatorNode> locatorNode = new AtomicReference<ILocatorNode>();
				Protocol.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						ILocatorModelLookupService lkup = ModelManager.getLocatorModel().getService(ILocatorModelLookupService.class);
						locatorNode.set(lkup.lkupLocatorNode(proxy));
					}
				});
				if (proxyInfo.length() > 0) {
					proxyInfo += " / "; //$NON-NLS-1$
				}
				String name = locatorNode.get() != null ? locatorNode.get().getPeer().getName() : proxy.getID();
				if (name == null || name.trim().length() == 0) {
					name = locatorNode.get() != null ? locatorNode.get().getPeer().getID() : proxy.getID();
				}
				else if (locatorNode.get() != null) {
					name += "  (" + locatorNode.get().getPeer().getAttributes().get(IPeer.ATTR_IP_HOST) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				}
	            proxyInfo += name.trim();
            }
			proxyControl.setEditFieldControlText(proxyInfo);
		}

		if (addressControl != null) {
			String ip = data.getStringProperty(IPeer.ATTR_IP_HOST);
			if (ip != null)
				addressControl.setEditFieldControlText(ip);
		}

		if (portControl != null) {
			String port = data.getStringProperty(IPeer.ATTR_IP_PORT);
			if (port != null)
				portControl.setEditFieldControlText(port);
		}

		setIsAutoPort(isAutoPort);

		// Mark the control update as completed now
		setIsUpdating(false);
		// Re-evaluate the dirty state
		dataChanged(null);
		// Adjust the control enablement
		updateEnablement();
    }

    /**
	 * Initialize the page widgets based of the data from the given peer node.
	 * <p>
	 * This method may called multiple times during the lifetime of the page and the given
	 * configuration node might be even <code>null</code>.
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
				// The section is handling the transport name and the
				// transport type specific properties. Ignore other properties.
				odc.setProperty(IPeer.ATTR_TRANSPORT_NAME, node.getPeer().getTransportName());
				odc.setProperty(IPeer.ATTR_IP_HOST, node.getPeer().getAttributes().get(IPeer.ATTR_IP_HOST));
				odc.setProperty(IPeer.ATTR_IP_PORT, node.getPeer().getAttributes().get(IPeer.ATTR_IP_PORT));
				odc.setProperty(IPeerProperties.PROP_IP_PORT_IS_AUTO, node.getPeer().getAttributes().get(IPeerProperties.PROP_IP_PORT_IS_AUTO));
				odc.setProperty(IPeerProperties.PROP_PROXIES, node.getPeer().getAttributes().get(IPeerProperties.PROP_PROXIES));

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

		boolean isAutoPort = isAutoPort();

		data.setProperty(IPeerProperties.PROP_PROXIES, proxies);

		if (addressControl != null) {
			String host = addressControl.getEditFieldControlText();
			data.setProperty(IPeer.ATTR_IP_HOST, !"".equals(host) ? host : null); //$NON-NLS-1$
		}

		if (portControl != null) {
			String port = portControl.getEditFieldControlText();
			if (isAutoPort) {
				data.setProperty(IPeer.ATTR_IP_PORT, null);
			}
			else {
				data.setProperty(IPeer.ATTR_IP_PORT, !"".equals(port) ? port : null); //$NON-NLS-1$
			}
		}

		if (isAutoPort) {
			data.setProperty(IPeerProperties.PROP_IP_PORT_IS_AUTO, Boolean.TRUE.toString());
		} else {
			data.setProperty(IPeerProperties.PROP_IP_PORT_IS_AUTO, null);
		}

		// Transport name is always "TCP"
		data.setProperty(IPeer.ATTR_TRANSPORT_NAME, ITransportTypes.TRANSPORT_TYPE_TCP);
    }

    /**
	 * Stores the page widgets current values to the given peer node.
	 * <p>
	 * This method may called multiple times during the lifetime of the page and the given peer node
	 * might be even <code>null</code>.
	 *
	 * @param node The peer model node or <code>null</code>.
	 */
	public void extractData(final IPeerNode node) {
		// If no data is available, we are done
		if (node == null) {
			return;
		}

		// The list of removed attributes
		final List<String> removed = new ArrayList<String>();
		// Get the current key set from the working copy
		Set<String> currentKeySet = wc.getProperties().keySet();

		extractData(wc);

		// If the data has not changed compared to the original data copy,
		// we are done here and return immediately
		if (odc.equals(wc)) {
			return;
		}

		// Get the new key set from the working copy
		Set<String> newKeySet = wc.getProperties().keySet();
		// Everything from the old key set not found in the new key set is a removed attribute
		for (String key : currentKeySet) {
			if (!newKeySet.contains(key)) {
				removed.add(key);
			}
		}

		// Copy the working copy data back to the original properties container
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				// To update the peer attributes, the peer needs to be recreated
				IPeer oldPeer = node.getPeer();
				// Create a write able copy of the peer attributes
				Map<String, String> attributes = new HashMap<String, String>(oldPeer.getAttributes());
				// Clean out the removed attributes
				for (String key : removed) {
					attributes.remove(key);
				}
				// Update with the current configured attributes
				for (String key : wc.getProperties().keySet()) {
					String value = wc.getStringProperty(key);
					if (value != null) {
						attributes.put(key, value);
					}
					else {
						attributes.remove(key);
					}
				}

				// If there is still a open channel to the old peer, close it by force
				IChannel channel = Tcf.getChannelManager().getChannel(oldPeer);
				if (channel != null) {
					channel.close();
				}

				// Create the new peer
				IPeer newPeer = new Peer(attributes);
				// Update the peer node instance (silently)
				boolean changed = node.setChangeEventsEnabled(false);
				node.setProperty(IPeerNodeProperties.PROPERTY_INSTANCE, newPeer);
				// As the transport changed, we have to reset the state back to "unknown"
				// and clear out the services and DNS markers
				node.setProperty("dns.name.transient", null); //$NON-NLS-1$
				node.setProperty("dns.lastIP.transient", null); //$NON-NLS-1$
				node.setProperty("dns.skip.transient", null); //$NON-NLS-1$

				if (changed) {
					node.setChangeEventsEnabled(true);
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#isValid()
	 */
	@Override
	public boolean isValid() {
		// Validation is skipped while the controls are updated
		if (isUpdating()) return true;

		boolean valid = super.isValid();

		if (addressControl != null) {
			valid &= addressControl.isValid();
			if (addressControl.getMessageType() > getMessageType()) {
				setMessage(addressControl.getMessage(), addressControl.getMessageType());
			}
		}

		if (portControl != null) {
			valid &= portControl.isValid();
			if (portControl.getMessageType() > getMessageType()) {
				setMessage(portControl.getMessage(), portControl.getMessageType());
			}
		}

		return valid;
	}

	/*
	 * (non-Javadoc)
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
		// Extract the data into the original data node
		extractData(od);
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

		String transportType = wc.getStringProperty(IPeer.ATTR_TRANSPORT_NAME);
		if ("".equals(transportType)) { //$NON-NLS-1$
			String value = odc.getStringProperty(IPeer.ATTR_TRANSPORT_NAME);
			isDirty |= value != null && !"".equals(value.trim()); //$NON-NLS-1$
		}
		else {
			isDirty |= !odc.isProperty(IPeer.ATTR_TRANSPORT_NAME, transportType);
		}

		if (addressControl != null) {
			String address = addressControl.getEditFieldControlText();
			if (address != null) {
				if ("".equals(address)) { //$NON-NLS-1$
					isDirty |= odc.getStringProperty(IPeer.ATTR_IP_HOST) != null && !address.equals(odc.getStringProperty(IPeer.ATTR_IP_HOST));
				} else {
					isDirty |= !address.equals(odc.getStringProperty(IPeer.ATTR_IP_HOST));
				}
			}
		}

		boolean autoPort = odc.getBooleanProperty(IPeerProperties.PROP_IP_PORT_IS_AUTO);
		if (!autoPort && portControl != null) {
			String port = portControl.getEditFieldControlText();
			String oldPort = odc.getStringProperty(IPeer.ATTR_IP_PORT);
			isDirty |= !port.equals(oldPort != null ? oldPort : ""); //$NON-NLS-1$
		}
		isDirty |= isAutoPort() != autoPort;

		String newProxies = odc.getStringProperty(IPeerProperties.PROP_PROXIES);
		if (proxies == null || proxies.trim().length() == 0) {
			isDirty |= newProxies != null && newProxies.trim().length() > 0;
		}
		else {
			isDirty |= newProxies == null || newProxies.trim().length() == 0;
		}

		// If dirty, mark the form part dirty.
		// Otherwise call refresh() to reset the dirty (and stale) flag
		markDirty(isDirty);

		// Adjust the control enablement
		updateEnablement();
	}

	/**
	 * Updates the given set of attributes with the current values of the page widgets.
	 *
	 * @param attributes The attributes to update. Must not be <code>null</code>:
	 */
	public void updateAttributes(IPropertiesContainer attributes) {
		Assert.isNotNull(attributes);

		attributes.setProperty(IPeerProperties.PROP_PROXIES, null);
		attributes.setProperty(IPeer.ATTR_IP_HOST, null);
		attributes.setProperty(IPeer.ATTR_IP_PORT, null);
		attributes.setProperty(IPeerProperties.PROP_IP_PORT_IS_AUTO, null);

		extractData(attributes);
	}

	/**
	 * Updates the control enablement.
	 */
	protected void updateEnablement() {
		// Determine the input
		final Object input = od; // getManagedForm().getInput();

		boolean enabled = !isReadOnly() && (!(input instanceof IPeerNode) || ((IPeerNode)input).getConnectState() == IConnectable.STATE_DISCONNECTED);
		if (proxyControl != null) {
			proxyControl.setEnabled(enabled);
			if (enabled) SWTControlUtil.setEnabled(proxyControl.getEditFieldControl(), false);
		}
		if (addressControl != null) addressControl.setEnabled(enabled);
		if (portControl != null) portControl.setEnabled(enabled && !isAutoPort);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#saveWidgetValues(org.eclipse.jface.dialogs.IDialogSettings)
	 */
	@Override
	public void saveWidgetValues(IDialogSettings settings) {
		super.saveWidgetValues(settings);

		if (settings != null) {
			if (addressControl != null) addressControl.saveWidgetValues(settings, TcpTransportSection.class.getSimpleName());
			if (portControl != null && !portControl.getEditFieldControlText().equals(getDefaultPort())) portControl.saveWidgetValues(settings, TcpTransportSection.class.getSimpleName());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.forms.parts.AbstractSection#restoreWidgetValues(org.eclipse.jface.dialogs.IDialogSettings)
	 */
	@Override
	public void restoreWidgetValues(IDialogSettings settings) {
		super.restoreWidgetValues(settings);

		if (settings != null) {
			if (addressControl != null) addressControl.restoreWidgetValues(settings, TcpTransportSection.class.getSimpleName());
			if (portControl != null) portControl.restoreWidgetValues(settings, TcpTransportSection.class.getSimpleName());
		}
	}
}
