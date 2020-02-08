/*******************************************************************************
 * Copyright (c) 2012, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.wizards.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.core.TransientPeer;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.core.interfaces.ITransportTypes;
import org.eclipse.tcf.te.tcf.core.util.persistence.PeerDataHelper;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelUpdateService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.controls.CustomTransportPanel;
import org.eclipse.tcf.te.tcf.ui.controls.PeerAttributesTablePart;
import org.eclipse.tcf.te.tcf.ui.controls.PeerNameControl;
import org.eclipse.tcf.te.tcf.ui.controls.PipeTransportPanel;
import org.eclipse.tcf.te.tcf.ui.controls.TcpTransportPanel;
import org.eclipse.tcf.te.tcf.ui.controls.TransportTypeControl;
import org.eclipse.tcf.te.tcf.ui.controls.TransportTypePanelControl;
import org.eclipse.tcf.te.tcf.ui.dialogs.LocatorNodeSelectionDialog;
import org.eclipse.tcf.te.tcf.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl;
import org.eclipse.tcf.te.ui.controls.interfaces.IWizardConfigurationPanel;
import org.eclipse.tcf.te.ui.controls.validator.RegexValidator;
import org.eclipse.tcf.te.ui.controls.validator.TextValidator;
import org.eclipse.tcf.te.ui.controls.validator.Validator;
import org.eclipse.tcf.te.ui.forms.FormLayoutFactory;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.tcf.te.ui.wizards.pages.AbstractValidatingWizardPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Wizard page implementation querying all information needed
 * to create the different TCF peer types.
 */
public class NewTargetWizardPage extends AbstractValidatingWizardPage implements IDataExchangeNode {
	private PeerNameControl peerNameControl;
	BaseEditBrowseTextControl proxyControl = null;
	TransportTypeControl transportTypeControl;
	TransportTypePanelControl transportTypePanelControl;
	private PeerAttributesTablePart tablePart;
	/* default */ Button connect = null;
	String proxies = null;

	private FormToolkit toolkit = null;

	/* default */ boolean autoConnect = false;

	// The UUID of the new peer to create
	private final UUID uuid = UUID.randomUUID();

	// The list of existing configuration names. Used to generate a unique name
	// and validate the wizard
	/* default */ final java.util.List<String> usedNames = new ArrayList<String>();

	/**
	 * Local transport type control implementation.
	 */
	private class MyTransportTypeControl extends TransportTypeControl {

		/**
		 * Constructor.
		 *
		 * @param parentPage The parent dialog page this control is embedded in.
		 *                   Might be <code>null</code> if the control is not associated with a page.
		 */
		public MyTransportTypeControl(IDialogPage parentPage) {
			super(parentPage);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetSelected(SelectionEvent e) {
			if (transportTypePanelControl != null) {
				transportTypePanelControl.showConfigurationPanel(getSelectedTransportType());
				validate();
			}
		}
	}

	/**
	 * Local transport type panel control implementation.
	 */
	private class MyTransportTypePanelControl extends TransportTypePanelControl {

		/**
		 * Constructor.
		 *
		 * @param parentPage The parent dialog page this control is embedded in.
		 *                   Might be <code>null</code> if the control is not associated with a page.
		 */
		public MyTransportTypePanelControl(IDialogPage parentPage) {
			super(parentPage);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.ui.controls.BaseControl#isValid()
		 */
		@Override
		public boolean isValid() {
			boolean valid = super.isValid();
			if (!valid) return false;

			// Get the currently selected transport type
			if (transportTypeControl != null) {
				String transportType = transportTypeControl.getSelectedTransportType();
				if (transportType != null) {
					// get the panel for the transport type and validate the panel
					IWizardConfigurationPanel panel = getConfigurationPanel(transportType);
					// getConfigurationPanel(...) always return a non-null value
					Assert.isNotNull(panel);
					valid = panel.isValid();
					setMessage(panel.getMessage(), panel.getMessageType());
				}
			}

			return valid;
		}
	}

	/**
	 * Constructor.
	 */
	public NewTargetWizardPage() {
		this(NewTargetWizardPage.class.getName());
	}

	/**
	 * Constructor.
	 *
	 * @param pageName The page name. Must not be <code>null</code>.
	 */
	public NewTargetWizardPage(String pageName) {
		super(pageName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.DialogPage#dispose()
	 */
	@Override
	public void dispose() {
		if (peerNameControl != null) { peerNameControl.dispose(); peerNameControl = null; }
		if (transportTypeControl != null) { transportTypeControl.dispose(); transportTypeControl = null; }
		if (transportTypePanelControl != null) { transportTypePanelControl.dispose(); transportTypePanelControl = null; }
		if (tablePart != null) { tablePart.dispose(); tablePart = null; }
		if (toolkit != null) { toolkit.dispose(); toolkit = null; }

	    super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		// Setup title and description
		setTitle(Messages.NewTargetWizardPage_title);
		setDescription(Messages.NewTargetWizardPage_description);

		// Create the forms toolkit
		toolkit = new FormToolkit(parent.getDisplay());

		// Create the main panel
		Composite mainPanel = toolkit.createComposite(parent);
		mainPanel.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		mainPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mainPanel.setBackground(parent.getBackground());

		setControl(mainPanel);

		// Setup the help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(mainPanel, IContextHelpIds.NEW_TARGET_WIZARD_PAGE);

		// Do not validate the page while creating the controls
		boolean changed = setValidationInProgress(true);
		// Create the main panel sub controls
		createMainPanelControls(mainPanel, toolkit);
		// Reset the validation in progress state
		if (changed) setValidationInProgress(false);

		// Adjust the font
		Dialog.applyDialogFont(mainPanel);

		// Validate the page for the first time
		validate();
	}

	/**
	 * Creates the main panel sub controls.
	 *
	 * @param parent The parent main panel composite. Must not be <code>null</code>.
	 * @param toolkit The form toolkit. Must not be <code>null</code>.
	 */
	protected void createMainPanelControls(Composite parent, FormToolkit toolkit) {
		Assert.isNotNull(parent);
		Assert.isNotNull(toolkit);

		// Create the client composite
		Composite client = toolkit.createComposite(parent);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));
		client.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		client.setBackground(parent.getBackground());

		// Add the controls
		peerNameControl = new PeerNameControl(this) {
			@Override
			public boolean isValid() {
				boolean valid = true;

				String name = getEditFieldControlTextForValidation();
				if (!"".equals(name)) { //$NON-NLS-1$
					// Name is not empty -> check against the list of used names
					if (getParentPage() instanceof NewTargetWizardPage) {
						valid = !((NewTargetWizardPage)getParentPage()).usedNames.contains(name.trim().toUpperCase());
						if (!valid) {
							setMessage(Messages.NewTargetWizardPage_error_nameInUse, IMessageProvider.ERROR);
						}
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
			@Override
			protected void onButtonControlSelected() {
				LocatorNodeSelectionDialog dialog = new LocatorNodeSelectionDialog(null, true) {
					@Override
					protected String getDialogTitle() {
					    return Messages.NewTargetWizardPage_PeerSelectionDialog_dialogTitle;
					}
					@Override
					protected String getTitle() {
					    return Messages.NewTargetWizardPage_PeerSelectionDialog_title;
					}
					@Override
					protected String getDefaultMessage() {
					    return Messages.NewTargetWizardPage_PeerSelectionDialog_message;
					}
				};

				ILocatorNode locatorNode = getLocatorNode();

				dialog.setSelection(locatorNode != null ? new StructuredSelection(locatorNode) : null);

				// Open the dialog
				if (dialog.open() == Window.OK) {
					// Get the selected proxy from the dialog
					ISelection selection = dialog.getSelection();
					if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
						if (((IStructuredSelection)selection).getFirstElement() instanceof ILocatorNode) {
							final IPeer peer = ((ILocatorNode)((IStructuredSelection)selection).getFirstElement()).getPeer();
							final IPropertiesContainer data = new PropertiesContainer();
							Protocol.invokeAndWait(new Runnable() {
								@Override
								public void run() {
									for (Entry<String, String> attribute : peer.getAttributes().entrySet()) {
										data.setProperty(attribute.getKey(), attribute.getValue());
									}
								}
							});
							setupData(data);
						}
						else {
							proxies = null;
							proxyControl.setEditFieldControlText(""); //$NON-NLS-1$
						}
					}
				}
			}
			/* (non-Javadoc)
			 * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#doCreateEditFieldValidator()
			 */
			@Override
			protected Validator doCreateEditFieldValidator() {
				return new RegexValidator(Validator.ATTR_MANDATORY, "[0-9a-zA-Z. _()-]+"); //$NON-NLS-1$
			}
			@Override
            protected void configureEditFieldValidator(Validator validator) {
				if (validator == null) return;
				validator.setMessageText(TextValidator.INFO_MISSING_NAME, Messages.NewTargetWizardPage_description);
			}
		};
		peerNameControl.setFormToolkit(toolkit);
		peerNameControl.setParentControlIsInnerPanel(false);
		peerNameControl.setHideBrowseButton(false);
		peerNameControl.setupPanel(client);
		peerNameControl.getEditFieldControl().setFocus();

		createEmptySpace(client, 5, 2, toolkit);

		proxyControl = new BaseEditBrowseTextControl(null);
		proxyControl.setFormToolkit(toolkit);
		proxyControl.setParentControlIsInnerPanel(false);
		proxyControl.setHideBrowseButton(true);
		proxyControl.setReadOnly(true);
		proxyControl.setIsGroup(false);
		proxyControl.setHasHistory(false);
		proxyControl.setEditFieldLabel(Messages.TcpTransportSection_proxies_label);
		proxyControl.setupPanel(client);
		SWTControlUtil.setEnabled(proxyControl.getEditFieldControl(), false);

		createEmptySpace(client, 5, 2, toolkit);

		// Create and configure the transport type section
		Section transportTypeSection = toolkit.createSection(client, ExpandableComposite.TITLE_BAR);
		transportTypeSection.setText(Messages.NewTargetWizardPage_section_transportType);
		transportTypeSection.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		layoutData.horizontalSpan = 2;
		transportTypeSection.setLayoutData(layoutData);
		transportTypeSection.setBackground(client.getBackground());

		Composite transportTypeClient = toolkit.createComposite(transportTypeSection);
		transportTypeClient.setLayout(new GridLayout());
		transportTypeClient.setBackground(transportTypeSection.getBackground());
		transportTypeSection.setClient(transportTypeClient);

		// Create the transport type control
		transportTypeControl = new MyTransportTypeControl(this);
		transportTypeControl.setFormToolkit(toolkit);
		transportTypeControl.setupPanel(transportTypeClient);

		// The transport type specific controls are placed into a stack
		transportTypePanelControl = new MyTransportTypePanelControl(this);

		// Create and add the panels
		TcpTransportPanel tcpTransportPanel = new TcpTransportPanel(transportTypePanelControl);
		transportTypePanelControl.setFormToolkit(toolkit);
		transportTypePanelControl.addConfigurationPanel(ITransportTypes.TRANSPORT_TYPE_TCP, tcpTransportPanel);
		transportTypePanelControl.addConfigurationPanel(ITransportTypes.TRANSPORT_TYPE_SSL, tcpTransportPanel);
		transportTypePanelControl.addConfigurationPanel(ITransportTypes.TRANSPORT_TYPE_PIPE, new PipeTransportPanel(transportTypePanelControl));
		transportTypePanelControl.addConfigurationPanel(ITransportTypes.TRANSPORT_TYPE_CUSTOM, new CustomTransportPanel(transportTypePanelControl));

		// Setup the panel control
		transportTypePanelControl.setupPanel(transportTypeClient, transportTypeControl.getTransportTypes(), toolkit);
		layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		layoutData.horizontalSpan = 2;
		transportTypePanelControl.getPanel().setLayoutData(layoutData);
		toolkit.adapt(transportTypePanelControl.getPanel());

		transportTypePanelControl.showConfigurationPanel(transportTypeControl.getSelectedTransportType());

		// Create the advanced peer properties table
		createPeerAttributesTableControl(client, toolkit);

		// Create the auto connect button
		if (hasAutoConnectButton()) {
			if (System.getProperty("NewWizard_" + IPeerProperties.PROP_AUTO_CONNECT) != null) { //$NON-NLS-1$
				autoConnect = Boolean.getBoolean("NewWizard_" + IPeerProperties.PROP_AUTO_CONNECT); //$NON-NLS-1$
			}

			connect = toolkit.createButton(client, Messages.AbstractConfigWizardPage_connect_label, SWT.CHECK);
			layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
			layoutData.horizontalSpan = 2;
			connect.setLayoutData(layoutData);
			connect.setSelection(autoConnect);
			connect.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					autoConnect = SWTControlUtil.getSelection(connect);
				}
			});
		}

		// restore the widget values from the history
		restoreWidgetValues();

		// Initialize the used configuration name list
		initializeUsedNameList();
	}

	protected ILocatorNode getLocatorNode() {
		final AtomicReference<ILocatorNode> selectedLocatorNode = new AtomicReference<ILocatorNode>();
		if (transportTypeControl.isValid() && transportTypePanelControl.isValid()) {
			final IPropertiesContainer transportData = new PropertiesContainer();
			extractData(transportData);
			Protocol.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					String proxy = transportData.getStringProperty(IPeerProperties.PROP_PROXIES);
					if (proxy == null || proxy.trim().length() == 0) {
						return;
					}
					String host = transportData.getStringProperty(IPeer.ATTR_IP_HOST);
					String port = transportData.getStringProperty(IPeer.ATTR_IP_PORT);
					String transport = transportData.getStringProperty(IPeer.ATTR_TRANSPORT_NAME);
					String id = transport + ":" + host + ":" + port; //$NON-NLS-1$ //$NON-NLS-2$
					Map<String, String> attrs = new HashMap<String, String>();
					attrs.put(IPeer.ATTR_ID, id);
					attrs.put(IPeer.ATTR_IP_HOST, host);
					attrs.put(IPeer.ATTR_IP_PORT, port);
					attrs.put(IPeer.ATTR_TRANSPORT_NAME, transport);
					attrs.put(IPeerProperties.PROP_PROXIES, proxy);
					IPeer peer = new TransientPeer(attrs);
					ILocatorModelLookupService lkup = ModelManager.getLocatorModel()
					                .getService(ILocatorModelLookupService.class);
					selectedLocatorNode.set(lkup.lkupLocatorNode(peer));
					if (selectedLocatorNode.get() == null) {
						ILocatorModelUpdateService update = ModelManager.getLocatorModel()
						                .getService(ILocatorModelUpdateService.class);
						selectedLocatorNode.set(update.add(peer, true));
					}
				}
			});
		}
		return selectedLocatorNode.get();
	}

	/**
	 * Creates the peer attributes table controls.
	 *
	 * @param parent The parent composite. Must not be <code>null</code>.
	 * @param toolkit The form toolkit. Must not be <code>null</code>.
	 */
	protected void createPeerAttributesTableControl(Composite parent, FormToolkit toolkit) {
		Assert.isNotNull(parent);

		createEmptySpace(parent, 5, 2, toolkit);

		// Create and configure the advanced attributes section
		Section attributesSection = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
		attributesSection.setText(Messages.NewTargetWizardPage_section_attributes);
		attributesSection.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		layoutData.horizontalSpan = 2;
		attributesSection.setLayoutData(layoutData);
		attributesSection.setBackground(parent.getBackground());

		Composite client = toolkit.createComposite(attributesSection);
		client.setLayout(new GridLayout(2, false));
		client.setBackground(attributesSection.getBackground());
		attributesSection.setClient(client);

		tablePart = new PeerAttributesTablePart();
		tablePart.setMinSize(SWTControlUtil.convertWidthInCharsToPixels(client, 20), SWTControlUtil.convertHeightInCharsToPixels(client, 6));
		tablePart.setBannedNames(new String[] { IPeer.ATTR_ID, IPeer.ATTR_AGENT_ID, IPeer.ATTR_SERVICE_MANGER_ID, IPeer.ATTR_NAME, IPeer.ATTR_TRANSPORT_NAME, IPeer.ATTR_IP_HOST, IPeer.ATTR_IP_PORT, "PipeName" }); //$NON-NLS-1$
		tablePart.createControl(client, SWT.SINGLE | SWT.FULL_SELECTION, 2, toolkit);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.wizards.pages.AbstractValidatingWizardPage#doValidate()
	 */
	@Override
	protected ValidationResult doValidate() {
		ValidationResult result = new ValidationResult();

		boolean valid = true;

		if (peerNameControl != null) {
			valid &= peerNameControl.isValid();
			result.setResult(peerNameControl);
		}

		if (transportTypeControl != null) {
			valid &= transportTypeControl.isValid();
			result.setResult(transportTypeControl);
		}

		if (transportTypePanelControl != null) {
			valid &= transportTypePanelControl.isValid();
			result.setResult(transportTypePanelControl);
		}

		result.setValid(valid);

		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#setupData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void setupData(IPropertiesContainer data) {

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
	            proxyInfo += name.trim();
            }
			proxyControl.setEditFieldControlText(proxyInfo);
		}

		if (data.containsKey(IPeer.ATTR_NAME) && peerNameControl != null) {
			String name = data.getStringProperty(IPeer.ATTR_NAME);
			int i = 1;
			while (usedNames.contains(name.toUpperCase())) {
				name = data.getStringProperty(IPeer.ATTR_NAME) + " (" + i + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				i++;
			}
			peerNameControl.setEditFieldControlText(name);
		}

		String transportType = data.getStringProperty(IPeer.ATTR_TRANSPORT_NAME);
		if (transportType != null) {
			if (transportTypeControl != null) {
				transportTypeControl.setSelectedTransportType(transportType);
				if (!transportTypeControl.getSelectedTransportType().equals(transportType)) {
					transportTypeControl.setSelectedTransportType(ITransportTypes.TRANSPORT_TYPE_CUSTOM);
				}
				transportTypePanelControl.showConfigurationPanel(transportTypeControl.getSelectedTransportType());
			}
			IWizardConfigurationPanel panel = transportTypePanelControl != null ? transportTypePanelControl.getActiveConfigurationPanel() : null;
			if (panel instanceof IDataExchangeNode) {
				((IDataExchangeNode)panel).setupData(data);
			}
		}
	}

	/**
	 * Updates the given attributes properties container with the current control content.
	 *
	 * @param peerAttributes The peer attributes. Must not be <code>null</code>.
	 */
	protected void updatePeerAttributes(IPropertiesContainer peerAttributes) {
		Assert.isNotNull(peerAttributes);

		// If the page has been never shown, we are done here
		if (getControl() == null) return;

		peerAttributes.setProperty(IPeer.ATTR_ID, uuid.toString());

		peerAttributes.setProperty(IPeerProperties.PROP_PROXIES, proxies);

		String value = peerNameControl != null ? peerNameControl.getEditFieldControlText() : null;
		if (value != null && !"".equals(value)) peerAttributes.setProperty(IPeer.ATTR_NAME, value); //$NON-NLS-1$

		value = transportTypeControl != null ? transportTypeControl.getSelectedTransportType() : null;
		if (value != null && !"".equals(value) && !ITransportTypes.TRANSPORT_TYPE_CUSTOM.equals(value)) { //$NON-NLS-1$
			peerAttributes.setProperty(IPeer.ATTR_TRANSPORT_NAME, value);
		}

		IWizardConfigurationPanel panel = transportTypePanelControl != null ? transportTypePanelControl.getConfigurationPanel(value) : null;
		if (panel instanceof IDataExchangeNode) {
			IPropertiesContainer data = new PropertiesContainer();
			((IDataExchangeNode)panel).extractData(data);

			// Copy all string properties to the peer attributes map
			for (String key : data.getProperties().keySet()) {
				value = data.getStringProperty(key);
				if (value != null && !"".equals(value)) peerAttributes.setProperty(key, value); //$NON-NLS-1$
			}
		}

		Map<String, String> additionalAttributes = tablePart != null ? tablePart.getAttributes() : null;
		if (additionalAttributes != null && !additionalAttributes.isEmpty()) {
			peerAttributes.addProperties(additionalAttributes);
		}

		if (isAutoConnect()) {
			peerAttributes.setProperty(IPeerProperties.PROP_AUTO_CONNECT, true);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode#extractData(org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	public void extractData(IPropertiesContainer data) {
		Assert.isNotNull(data);
		// Update with the current control content
		updatePeerAttributes(data);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.wizards.pages.AbstractWizardPage#saveWidgetValues()
	 */
	@Override
	public void saveWidgetValues() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			if (peerNameControl != null) peerNameControl.saveWidgetValues(settings, null);
			if (transportTypeControl != null) transportTypeControl.saveWidgetValues(settings, null);
			if (transportTypePanelControl != null) transportTypePanelControl.saveWidgetValues(settings, null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.wizards.pages.AbstractWizardPage#restoreWidgetValues()
	 */
	@Override
	public void restoreWidgetValues() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			if (peerNameControl != null) peerNameControl.restoreWidgetValues(settings, null);
			if (transportTypeControl != null) transportTypeControl.restoreWidgetValues(settings, null);
			if (transportTypePanelControl != null) transportTypePanelControl.restoreWidgetValues(settings, null);
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
						String name = peerNode.getPeer().getName();
						Assert.isNotNull(name);
						if (!"".equals(name) && !usedNames.contains(name)) { //$NON-NLS-1$
							usedNames.add(name.trim().toUpperCase());
						}
				}
			}
		};

		Assert.isTrue(!Protocol.isDispatchThread());
		Protocol.invokeAndWait(runnable);
	}

	/**
	 * Returns if or if not the wizard page should have an auto connect button.
	 *
	 * @return <code>True</code> if the page should have an auto connect button, <code>false</code> otherwise.
	 */
	protected boolean hasAutoConnectButton() {
		return true;
	}

	/**
	 * Returns if or if not to connect after the configuration got created.
	 *
	 * @return <code>True</code> to connect, <code>false</code> if not.
	 */
	public final boolean isAutoConnect() {
		return autoConnect;
	}
}
