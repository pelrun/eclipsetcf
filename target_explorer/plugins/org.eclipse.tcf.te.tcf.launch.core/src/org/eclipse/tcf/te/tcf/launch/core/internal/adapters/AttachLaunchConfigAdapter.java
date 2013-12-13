/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.launch.core.internal.adapters;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.tcf.te.launch.core.lm.LaunchManager;
import org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchManagerDelegate;
import org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchSpecification;
import org.eclipse.tcf.te.launch.core.selection.LaunchSelection;
import org.eclipse.tcf.te.launch.core.selection.RemoteSelectionContext;
import org.eclipse.tcf.te.launch.core.selection.interfaces.ILaunchSelection;
import org.eclipse.tcf.te.tcf.launch.core.interfaces.ILaunchTypes;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * AttachLaunchConfigAdapter
 */
public class AttachLaunchConfigAdapter {

	public ILaunchConfiguration getAttachLaunchConfig(IPeerNode peer) {
		ILaunchConfigurationType launchConfigType =	DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(ILaunchTypes.ATTACH);
		ILaunchSelection launchSelection = new LaunchSelection(ILaunchManager.DEBUG_MODE, new RemoteSelectionContext(peer, true));
		ILaunchManagerDelegate delegate = LaunchManager.getInstance().getLaunchManagerDelegate(launchConfigType, ILaunchManager.DEBUG_MODE);
		// create an empty launch configuration specification to initialize all attributes with their default defaults.
		ILaunchSpecification launchSpec = delegate.getLaunchSpecification(launchConfigType.getIdentifier(), launchSelection);
		ILaunchConfiguration[] launchConfigs = null;

		try {
			launchConfigs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(launchConfigType);
			launchConfigs = delegate.getMatchingLaunchConfigurations(launchSpec, launchConfigs);
		}
		catch (Exception e)	{
		}

		ILaunchConfiguration config = launchConfigs != null && launchConfigs.length > 0 ? launchConfigs[0] : null;

		try {
			config = LaunchManager.getInstance().createOrUpdateLaunchConfiguration(config, launchSpec);
		}
		catch (Exception e) {
		}

		return config;
	}
}
