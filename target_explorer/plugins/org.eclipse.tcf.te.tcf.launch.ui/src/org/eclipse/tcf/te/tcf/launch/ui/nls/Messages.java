/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.nls;

import java.lang.reflect.Field;

import org.eclipse.osgi.util.NLS;

/**
 * TCF Launch UI Plug-in externalized strings management.
 */
public class Messages extends NLS {

	// The plug-in resource bundle name
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.tcf.launch.ui.nls.Messages"; //$NON-NLS-1$

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

	public static String AttachMainTab_title;
	public static String AttachMainTab_label;

	public static String LaunchConfigurationMainTabSection_title;
	public static String LaunchConfigurationMainTabSection_processArguments_label;
	public static String LaunchConfigurationMainTabSection_processImage_label;
	public static String LaunchConfigurationMainTabSection_error_missingProcessImage;
	public static String LaunchConfigurationMainTabSection_stopAtEntry_label;
	public static String LaunchConfigurationMainTabSection_stopAtMain_label;
	public static String LaunchConfigurationMainTabSection_attachChildren_label;

	public static String AddEditFileTransferDialog_add_dialogTitle;
	public static String AddEditFileTransferDialog_edit_dialogTitle;
	public static String AddEditFileTransferDialog_add_title;
	public static String AddEditFileTransferDialog_edit_title;
	public static String AddEditFileTransferDialog_add_message;
	public static String AddEditFileTransferDialog_edit_message;
	public static String AddEditFileTransferDialog_target_label;
	public static String AddEditFileTransferDialog_host_label;
	public static String AddEditFileTransferDialog_options_label;
	public static String AddEditFileTransferDialog_toHost_checkbox;
	public static String AddEditFileTransferDialog_toTarget_checkbox;

	public static String LaunchConfigurationAdvancedTabSection_title;
	public static String LaunchConfigurationAdvancedTabSection_group_label;
	public static String LaunchConfigurationAdvancedTabSection_lineseparator_label;
	public static String LaunchConfigurationAdvancedTabSection_lineseparator_default;
	public static String LaunchConfigurationAdvancedTabSection_lineseparator_lf;
	public static String LaunchConfigurationAdvancedTabSection_lineseparator_crlf;
	public static String LaunchConfigurationAdvancedTabSection_lineseparator_cr;

	public static String PathMapEditorPage_name;
	public static String PathMapEditorPage_error_apply;
	public static String PathMapEditorPage_error_title;
	public static String PathMapEditorPage_showAll_label;
	public static String PathMapTab_copy_menu_label;

	public static String SourceLookupEditorPage_name;

	public static String AbstractDiagnosticsCommandHandler_progress_title;
	public static String AbstractDiagnosticsCommandHandler_progress_button_cancel;
}
