/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.remote.core.nls;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	// The plug-in resource bundle name
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.tcf.remote.core.nls.Messages"; //$NON-NLS-1$

	/**
	 * Static constructor.
	 */
	static {
		// Load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	// **** Declare externalized string id's down here *****

	public static String PeerInfo_errorCannotOpenChannel;
	public static String PeerInfo_errorNoFileSystemService;
	public static String PeerInfo_errorPeerNotConnected;

	public static String TCFFileStoreOperation_errorNotConnected;

	public static String TCFOperationGetEnvironment_errorNoChannel;
	public static String TCFOperationGetEnvironment_errorNoProcessesService;
}
