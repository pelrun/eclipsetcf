/*******************************************************************************
 * Copyright (c) 2011, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.nls;

import java.lang.reflect.Field;

import org.eclipse.osgi.util.NLS;

/**
 * TCF UI Plug-in externalized strings management.
 */
public class Messages extends NLS {

	// The plug-in resource bundle name
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.tcf.ui.nls.Messages"; //$NON-NLS-1$

	/**
	 * Static constructor.
	 */
	static {
		// Load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	/**
	 * Returns if or if not this NLS manager contains a constant for
	 * the given externalized strings key.
	 *
	 * @param key The externalized strings key or <code>null</code>.
	 * @return <code>True</code> if a constant for the given key exists, <code>false</code> otherwise.
	 */
	public static boolean hasString(String key) {
		if (key != null) {
			try {
				Field field = Messages.class.getDeclaredField(key);
				return field != null;
			} catch (NoSuchFieldException e) { /* ignored on purpose */ }
		}

		return false;
	}

	/**
	 * Returns the corresponding string for the given externalized strings
	 * key or <code>null</code> if the key does not exist.
	 *
	 * @param key The externalized strings key or <code>null</code>.
	 * @return The corresponding string or <code>null</code>.
	 */
	public static String getString(String key) {
		if (key != null) {
			try {
				Field field = Messages.class.getDeclaredField(key);
				return (String)field.get(null);
			} catch (Exception e) { /* ignored on purpose */ }
		}

		return null;
	}

	// **** Declare externalized string id's down here *****

	public static String PossibleCause;

	public static String ContentProvider_newNode;

	public static String OverviewEditorPage_title;
	public static String OverviewEditorPage_error_save;

	public static String AbstractCustomFormToolkitEditorPage_setAsDefault_link;

	public static String GeneralInformationSection_title;
	public static String GeneralInformationSection_description;
	public static String GeneralInformationSection_error_nameInUse;
	public static String GeneralInformationSection_error_delete;
	public static String GeneralInformationSection_error_emptyName;

	public static String TransportSection_title;
	public static String TransportSection_description;

	public static String TcpTransportSection_title;
	public static String TcpTransportSection_description;
	public static String TcpTransportSection_proxies_label;

	public static String ServicesSection_title;
	public static String ServicesSection_description;
	public static String ServicesSection_group_local_title;
	public static String ServicesSection_group_remote_title;

	public static String AttributesSection_title;
	public static String AttributesSection_description;

	public static String ContentProviderDelegate_newNode;

	public static String NewTargetWizard_windowTitle;
	public static String NewTargetWizard_newPeer_name;
	public static String NewTargetWizard_error_savePeer;

	public static String NewTargetWizardPage_title;
	public static String NewTargetWizardPage_description;
	public static String NewTargetWizardPage_section_transportType;
	public static String NewTargetWizardPage_section_attributes;
	public static String NewTargetWizardPage_error_nameInUse;
	public static String NewTargetWizardPage_PeerSelectionDialog_dialogTitle;
	public static String NewTargetWizardPage_PeerSelectionDialog_title;
	public static String NewTargetWizardPage_PeerSelectionDialog_message;

	public static String PeerIdControl_label;

	public static String PeerNameControl_label;
	public static String PeerNameControl_Information_MissingName;

	public static String TransportTypeControl_label;
	public static String TransportTypeControl_tcpType_label;
	public static String TransportTypeControl_sslType_label;
	public static String TransportTypeControl_pipeType_label;
	public static String TransportTypeControl_customType_label;

	public static String MyNetworkAddressControl_label;
	public static String MyNetworkAddressControl_information_missingTargetNameAddress;
	public static String MyNetworkAddressControl_error_invalidTargetNameAddress;
	public static String MyNetworkAddressControl_error_invalidTargetIpAddress;
	public static String MyNetworkAddressControl_information_checkNameAddressUserInformation;

	public static String MyRemoteHostAddressControl_label;

	public static String PipeNameControl_label;
	public static String PipeNameControl_information_missingValue;
	public static String PipeNameControl_error_invalidValue;

	public static String CustomTransportNameControl_label;
	public static String CustomTransportNameControl_information_missingValue;
	public static String CustomTransportNameControl_error_invalidValue;

	public static String PeerAttributesTablePart_button_new;
	public static String PeerAttributesTablePart_button_edit;
	public static String PeerAttributesTablePart_button_remove;
	public static String PeerAttributesTablePart_column_name;
	public static String PeerAttributesTablePart_column_value;
	public static String PeerAttributesTablePart_add_dialogTitle;
	public static String PeerAttributesTablePart_add_title;
	public static String PeerAttributesTablePart_add_message;
	public static String PeerAttributesTablePart_edit_dialogTitle;
	public static String PeerAttributesTablePart_edit_title;
	public static String PeerAttributesTablePart_edit_message;

	public static String SelectDefaultContextHandler_dialog_title;
	public static String SelectDefaultContextHandler_dialog_message;

	public static String DeleteHandler_error_title;
	public static String DeleteHandler_error_deleteFailed;

	public static String DeleteHandlerDelegate_DialogTitle;

	public static String DeleteHandlerDelegate_MsgDeleteMultiplePeers;

	public static String DeleteHandlerDelegate_MsgDeleteOnePeer;

	public static String LocatorNodeSelectionDialog_dialogTitle;
	public static String LocatorNodeSelectionDialog_title;
	public static String LocatorNodeSelectionDialog_message;
	public static String LocatorNodeSelectionDialog_button_add;
	public static String LocatorNodeSelectionDialog_button_delete;
	public static String LocatorNodeSelectionDialog_button_refresh;

	public static String LocatorNodeSelectionDialog_add_dialogTitle;
	public static String LocatorNodeSelectionDialog_add_title;
	public static String LocatorNodeSelectionDialog_add_message;

	public static String PeerNodeSelectionDialog_dialogTitle;
	public static String PeerNodeSelectionDialog_title;
	public static String PeerNodeSelectionDialog_message;

	public static String ActionHistorySelectionDialog_dialogTitle;
	public static String ActionHistorySelectionDialog_title;
	public static String ActionHistorySelectionDialog_message;
	public static String ActionHistorySelectionDialog_button_edit;
	public static String ActionHistorySelectionDialog_button_copy;
	public static String ActionHistorySelectionDialog_button_execute;
	public static String ActionHistorySelectionDialog_button_remove;

	public static String RedirectHandler_error_title;
	public static String RedirectHandler_error_redirectFailed;

	public static String RedirectAgentSelectionDialog_dialogTitle;
	public static String RedirectAgentSelectionDialog_title;
	public static String RedirectAgentSelectionDialog_message;

	public static String ResetRedirectHandler_error_title;
	public static String ResetRedirectHandler_error_resetRedirectFailed;

	public static String OfflineCommandHandler_error_title;
	public static String OfflineCommandHandler_error_makeOffline_failed;

	public static String CategoryManager_dnd_failed;

	public static String ConnectableToolbarCommandHandler_tooltip_connect;
	public static String ConnectableToolbarCommandHandler_tooltip_disconnect;

	public static String RenameHandler_error_title;
	public static String RenameHandler_error_renameFailed;
	public static String RenameHandler_dialog_title;
	public static String RenameHandler_dialog_message;
	public static String RenameHandler_dialog_error_nameExist;
	public static String RenameHandler_dialog_error_nameFormat;
	public static String RenameHandler_dialog_promptNewName;

	public static String TargetSelectorSection_title;
	public static String TargetSelectorSection_button_enableSimulator;
	public static String TargetSelectorSection_button_configure;
	public static String TargetSelectorSection_button_enableReal;

	public static String AbstractConfigurationEditorPage_error_possibleCause;
	public static String AbstractConfigurationEditorPage_error_save;

	public static String AbstractConfigWizardPage_configName_label;
	public static String AbstractConfigWizardPage_configName_infoMissingValue;
	public static String AbstractConfigWizardPage_configName_nameInUse;
	public static String AbstractConfigWizardPage_launchDbg_label;
	public static String AbstractConfigWizardPage_connect_label;
	public static String AbstractConfigWizardPage_advancedButton_label;

	public static String DefaultContextSelectorToolbarContribution_tooltip_new;
	public static String DefaultContextSelectorToolbarContribution_label_new;
	public static String DefaultContextSelectorToolbarContribution_tooltip_button;
	public static String DefaultContextSelectorToolbarContribution_tooltip_warningFix;

	public static String LoggingPreferencePage_label;
	public static String LoggingPreferencePage_enabled_label;
	public static String LoggingPreferencePage_monitorEnabled_label;
	public static String LoggingPreferencePage_filterGroup_label;
	public static String LoggingPreferencePage_showLocatorEvents_label;
	public static String LoggingPreferencePage_showHeartbeats_label;
	public static String LoggingPreferencePage_showFrameworkEvents_label;
	public static String LoggingPreferencePage_logfileGroup_label;
	public static String LoggingPreferencePage_maxFileSize_label;
	public static String LoggingPreferencePage_maxFileSize_error;
	public static String LoggingPreferencePage_maxFilesInCycle_label;

	public static String EditorSaveAsAdapter_title;
	public static String EditorSaveAsAdapter_message;
	public static String EditorSaveAsAdapter_nameInUse_error;
	public static String EditorSaveAsAdapter_label;

	public static String PeerExportWizard_title;
	public static String PeerExportWizard_message;
	public static String PeerExportWizardPage_selectAll;
	public static String PeerExportWizardPage_deselectAll;
	public static String PeerExportWizardPage_peers_label;
	public static String PeerExportWizardPage_destination_label;
	public static String PeerExportWizardPage_destination_button;
	public static String PeerExportWizardPage_peersMissing_error;
	public static String PeerExportWizardPage_destinationMissing_error;
	public static String PeerExportWizardPage_destinationIsFile_error;
	public static String PeerExportWizardPage_overwrite_button;
	public static String PeerExportWizardPage_overwriteDialog_message;
	public static String PeerExportWizardPage_overwriteDialogToggle_message;

	public static String PeerImportWizard_title;
	public static String PeerImportWizard_message;
	public static String PeerImportWizardPage_selectAll;
	public static String PeerImportWizardPage_deselectAll;
	public static String PeerImportWizardPage_peers_label;
	public static String PeerImportWizardPage_destination_label;
	public static String PeerImportWizardPage_destination_button;
	public static String PeerImportWizardPage_peersMissing_error;
	public static String PeerImportWizardPage_locationMissing_error;
	public static String PeerImportWizardPage_locationIsFile_error;
	public static String PeerImportWizardPage_overwrite_button;
	public static String PeerImportWizardPage_overwriteDialog_message;
	public static String PeerImportWizardPage_overwriteDialogToggle_message;

	public static String AbstractMapPropertiesSection_name_label;
	public static String AbstractMapPropertiesSection_value_label;

	public static String PeerNodePropertiesSection_title;
	public static String PeerPropertiesSection_title;

	public static String PingTimeoutSection_title;
	public static String PingTimeoutSection_timeout_label;

	public static String PeerLabelProviderDelegate_NodePropertiesTable_Mode_run;
	public static String PeerLabelProviderDelegate_NodePropertiesTable_Mode_stop;
	public static String PeerLabelProviderDelegate_NodePropertiesTable_SubType_real;
	public static String PeerLabelProviderDelegate_NodePropertiesTable_SubType_sim;
	public static String PeerLabelProviderDelegate_NodePropertiesTable_Port_0;
	public static String PeerLabelProviderDelegate_description_invalid;
}
