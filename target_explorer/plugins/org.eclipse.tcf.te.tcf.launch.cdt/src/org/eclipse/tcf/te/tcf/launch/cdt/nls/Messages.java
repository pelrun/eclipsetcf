/*******************************************************************************
 * Copyright (c) 2006, 2016 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Martin Oberhuber      (Wind River) - initial API and implementation
 * Ewa Matejska          (PalmSource) - [158783] browse button for cdt remote path
 * Johann Draschwandtner (Wind River) - [231827][remotecdt]Auto-compute default for Remote path
 * Anna Dushistova       (MontaVista) - [244173][remotecdt][nls] Externalize Strings in RemoteRunLaunchDelegate
 * Anna Dushistova       (MontaVista) - [181517][usability] Specify commands to be run before remote application launch
 * Nikita Shulga      (EmbeddedAlley) - [265236][remotecdt] Wait for RSE to initialize before querying it for host list
 * Anna Dushistova       (MontaVista) - [368597][remote debug] if gdbserver fails to launch on target, launch doesn't get terminated
 * Anna Dushistova       (MontaVista) - adapted from org.eclipse.cdt.launch.remote
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.nls;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.tcf.te.tcf.launch.cdt.nls.Messages"; //$NON-NLS-1$

	public static String Gdbserver_name_textfield_label;
	public static String Gdbserver_Settings_Tab_Name;

	public static String Port_number_textfield_label;
	public static String Port_number_mapped_to_textfield_label;

	public static String Remote_GDB_Debugger_Options;

	public static String RemoteCMainTab_Prerun;
	public static String RemoteCMainTab_Program;
	public static String RemoteCMainTab_Remote_Path_Browse_Button;
	public static String RemoteCMainTab_SkipDownload;
	public static String RemoteCMainTab_ErrorNoProgram;
	public static String RemoteCMainTab_ErrorNoConnection;
	public static String RemoteCMainTab_ErrorSymbolicLink;
	public static String RemoteCMainTab_Pid;
	public static String RemoteCMainTab_Prerun_Edit_Button;
	public static String RemoteCMainTab_Prerun_Edit_Dialog_Title;
	public static String RemoteCMainTab_RemoteUser_Label;

	public static String TEHelper_executing;
	public static String TEHelper_connection_not_found;

	public static String TCFPeerSelector_0;

	public static String TEGdbAbstractLaunchDelegate_no_program;
	public static String TEGdbAbstractLaunchDelegate_no_remote_path;
	public static String TEGdbAbstractLaunchDelegate_no_pid;
	public static String TEGdbAbstractLaunchDelegate_downloading;
	public static String TEGdbAbstractLaunchDelegate_attaching_program;
	public static String TEGdbAbstractLaunchDelegate_starting_program;
	public static String TEGdbAbstractLaunchDelegate_starting_debugger;
	public static String TEGdbAbstractLaunchDelegate_canceledMsg;
	public static String TEGdbAbstractLaunchDelegate_filetransferFailed;
	public static String TEGdbAbstractLaunchDelegate_gdbserverFailedToStartErrorMessage;
	public static String TEGdbAbstractLaunchDelegate_gdbserverFailedToStartErrorWithDetails;
	public static String TEGdbAbstractLaunchDelegate_error_addressInUse;
	public static String TEGdbAbstractLaunchDelegate_error_nosuchfileordirectory;
	public static String TEGdbAbstractLaunchDelegate_prerunScriptCreationFailed;
	public static String TEGdbAbstractLaunchDelegate_prerunScriptTransferFailed;
	public static String TEGdbAbstractLaunchDelegate_error_prerunScriptTemplate;

	public static String GdbPreferencePage_label;
	public static String GdbPreferencePage_portList_label;
	public static String GdbPreferencePage_mappedToPortList_label;
	public static String GdbPreferencePage_appLaunchGroup_label;
	public static String GdbPreferencePage_attachLaunchGroup_label;
	public static String GdbPreferencePage_portList_error;
	public static String GdbPreferencePage_portList_error_portList;
	public static String GdbPreferencePage_portList_error_mappedToPortList;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
