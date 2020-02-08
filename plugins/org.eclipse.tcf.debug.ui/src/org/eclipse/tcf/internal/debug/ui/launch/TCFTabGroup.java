/*******************************************************************************
 * Copyright (c) 2007, 2013 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.debug.ui.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;

/**
 * Launch configuration dialog tab group for Target Communication Framework
 */
public class TCFTabGroup extends AbstractLaunchConfigurationTabGroup {

    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        setTabs(new ILaunchConfigurationTab[] {
            new TCFTargetTab(),
            new TCFDownloadTab(),
            new TCFMainTab(),
            new TCFArgumentsTab(),
            new EnvironmentTab(),
            new TCFMemoryMapTab(),
            new TCFPathMapTab(),
            new SourceLookupTab(),
            new CommonTab()
        });
    }
}
