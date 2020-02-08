/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.nls;

import java.lang.reflect.Field;

import org.eclipse.osgi.util.NLS;

/**
 * Target Explorer UI plugin externalized strings management.
 */
public class Messages extends NLS {

	// The plug-in resource bundle name
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.ui.views.nls.Messages"; //$NON-NLS-1$

	/**
	 * Static constructor.
	 */
	static {
		// Load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
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

	// **** Declare externalized string id's down here *****

	public static String NewActionProvider_NewMenu_label;
	public static String NewActionProvider_NewWizardCommandAction_label;
	public static String NewActionProvider_NewWizardCommandAction_tooltip;

	public static String PropertiesCommandHandler_error_initPartFailed;

	public static String OpenCommandHandler_error_openEditor;

	public static String AddToCategoryAction_single_text;

	public static String RemoveFromCategoryAction_single_text;

	public static String RestoreJob_JobName;
	public static String RestoreJob_MainTask;
	public static String RestoreJob_Task1Name;
	public static String RestoreJob_Task2Name;

	public static String AbstractCustomFormToolkitEditorPage_HelpAction_label;
	public static String AbstractCustomFormToolkitEditorPage_HelpAction_tooltip;
	public static String AbstractCustomFormToolkitEditorPage_ApplyAction_label;
	public static String AbstractCustomFormToolkitEditorPage_ApplyAction_tooltip;

	public static String ButtonPanelControl_applyButton_label;

	public static String CommonFilterDescriptorLabelProvider_ContentExtensionDescription;

	public static String ConfigContentHandler_DialogTitle;
	public static String ConfigContentHandler_InitialFilter;
	public static String ConfigContentHandler_PromptMessage;
	public static String ConfigFiltersHandler_DialogTitle;
	public static String ConfigFiltersHandler_InitialFilter;
	public static String ConfigFiltersHandler_PromptMessage;

	public static String UpdateActiveExtensionsOperation_OperationName;
	public static String UpdateActiveFiltersOperation_OperationName;

	public static String ViewsUtil_reopen_error;

	public static String AbstractContextSelectorControl_error_noContextSelected_single;
	public static String AbstractContextSelectorControl_error_noContextSelected_multi;

	public static String AbstractContextSelectorSection_toolbar_refresh_tooltip;
	public static String AbstractContextSelectorSection_title;

}
