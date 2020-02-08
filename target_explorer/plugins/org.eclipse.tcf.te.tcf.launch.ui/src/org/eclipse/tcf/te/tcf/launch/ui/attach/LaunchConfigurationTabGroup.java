/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.attach;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.tcf.te.launch.ui.tabs.AbstractLaunchConfigurationTabGroup;

/**
 * Remote application launch configuration tab group implementation.
 */
public class LaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.tabs.AbstractLaunchContextConfigurationTabGroup#createContextSelectorTab(org.eclipse.debug.ui.ILaunchConfigurationDialog, java.util.List, java.lang.String)
	 */
	@Override
	public void createContextSelectorTab(ILaunchConfigurationDialog dialog, List<ILaunchConfigurationTab> tabs, String mode) {
		Assert.isNotNull(tabs);

		//		ILaunchConfigurationTab tab = new LaunchConfigurationMainTab();
		//		tabs.add(tab);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.tabs.AbstractLaunchContextConfigurationTabGroup#createAdditionalTabs(org.eclipse.debug.ui.ILaunchConfigurationDialog, java.util.List, java.lang.String)
	 */
	@Override
	public void createAdditionalTabs(ILaunchConfigurationDialog dialog, List<ILaunchConfigurationTab> tabs, String mode) {
		tabs.add(new LaunchConfigurationMainTab());
	}
}
