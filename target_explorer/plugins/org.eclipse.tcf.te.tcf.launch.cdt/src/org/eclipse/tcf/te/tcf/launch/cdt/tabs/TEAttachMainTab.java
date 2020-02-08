/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.tabs;

import org.eclipse.cdt.dsf.gdb.internal.ui.launching.CMainTab;

/**
 * Main attach tab implementation.
 */
@SuppressWarnings("restriction")
public class TEAttachMainTab extends TEAbstractMainTab {

	/**
     * Constructor
     */
    public TEAttachMainTab() {
    	super(CMainTab.DONT_CHECK_PROGRAM | PID_GROUP | NO_DOWNLOAD_GROUP | NO_PRERUN_GROUP);
    }

	@Override
	public String getId() {
		return "org.eclipse.tcf.te.remotecdt.attach.mainTab"; //$NON-NLS-1$
	}

}
