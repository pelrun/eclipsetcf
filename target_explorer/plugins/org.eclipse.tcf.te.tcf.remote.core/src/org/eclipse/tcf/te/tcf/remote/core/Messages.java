/*******************************************************************************
 * Copyright (c) 2014, 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.remote.core;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.tcf.remote.core.Messages"; //$NON-NLS-1$
	public static String TCFConnection_errorNoCommandShell;
	public static String TCFConnection_errorNoEnvironment;
	public static String TCFConnection_notifyListeners;
	public static String TCFConnectionBase_errorNoPortForwarding;
	public static String TCFConnectionManager_errorCannotConnect;
	public static String TCFConnectionManager_errorNoCreateConnection;
	public static String TCFFileManager_errorFileStoreForPath;
	public static String TCFProcessBuilder_errorConnectionClosed;
	public static String TCFProcessBuilder_errorLaunchingProcess;
	public static String TCFProcessBuilder_errorNoCommand;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
