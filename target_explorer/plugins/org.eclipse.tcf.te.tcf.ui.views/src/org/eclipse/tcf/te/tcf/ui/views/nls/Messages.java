/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.views.nls;

import org.eclipse.osgi.util.NLS;

/**
 * TCF Views Plug-in externalized strings management.
 */
public class Messages extends NLS {

	// The plug-in resource bundle name
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.tcf.ui.views.nls.Messages"; //$NON-NLS-1$

	/**
	 * Static constructor.
	 */
	static {
		// Load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	// **** Declare externalized string id's down here *****

	public static String ScriptPad_Console_name;

	public static String Monitor_Console_name;
	public static String Monitor_Console_name_with_peer;

	public static String ScriptPad_error_title;
	public static String ScriptPad_error_possibleCause;
	public static String ScriptPad_error_openFile;
	public static String ScriptPad_error_saveFile;
}
