/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.runtime.nls;

import org.eclipse.osgi.util.NLS;

/**
 * Target Explorer Runtime plugin externalized strings management.
 */
public class Messages extends NLS {

	// The plug-in resource bundle name
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.runtime.nls.Messages"; //$NON-NLS-1$

	/**
	 * Static constructor.
	 */
	static {
		// Load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	// **** Declare externalized string id's down here *****

	public static String Extension_error_missingRequiredAttribute;
	public static String Extension_error_duplicateExtension;
	public static String Extension_error_invalidExtensionPoint;

	public static String Callback_warning_cancelTimeout;
}
