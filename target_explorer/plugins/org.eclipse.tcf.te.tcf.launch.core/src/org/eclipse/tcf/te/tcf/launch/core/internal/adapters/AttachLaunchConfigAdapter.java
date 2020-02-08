/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.launch.core.internal.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.launch.core.lm.LaunchManager;
import org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchManagerDelegate;
import org.eclipse.tcf.te.launch.core.lm.interfaces.ILaunchSpecification;
import org.eclipse.tcf.te.launch.core.selection.LaunchSelection;
import org.eclipse.tcf.te.launch.core.selection.RemoteSelectionContext;
import org.eclipse.tcf.te.launch.core.selection.interfaces.ILaunchSelection;
import org.eclipse.tcf.te.tcf.launch.core.interfaces.ILaunchTypes;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;

/**
 * AttachLaunchConfigAdapter
 */
public class AttachLaunchConfigAdapter {

	public ILaunchConfiguration getAttachLaunchConfig(final IPeerNode peer) {
		final AtomicReference<String> name = new AtomicReference<String>();
		final AtomicBoolean isDeleted = new AtomicBoolean();
		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				isDeleted.set(peer.getBooleanProperty(IPeerNodeProperties.PROPERTY_IS_DELETED));
				name.set(peer.getName());
			}
		});

		ILaunchConfigurationType launchConfigType =	DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(ILaunchTypes.ATTACH);
		ILaunchSelection launchSelection = new LaunchSelection(ILaunchManager.DEBUG_MODE, new RemoteSelectionContext(peer, true));
		ILaunchManagerDelegate delegate = LaunchManager.getInstance().getLaunchManagerDelegate(launchConfigType, ILaunchManager.DEBUG_MODE);
		// create an empty launch configuration specification to initialize all attributes with their default defaults.
		ILaunchSpecification launchSpec = delegate.getLaunchSpecification(launchConfigType.getIdentifier(), launchSelection);
		ILaunchConfiguration[] launchConfigs = null;

		try {
			launchConfigs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(launchConfigType);
			// WB4-5394: Using getMatchingLaunchConfiguration is too heavy weight here as it does a lot
			//           XML parsing. It's more accurate, but the attach launch configuration has the
			//           same name as the connection, so we look for the connection name first.
			List<ILaunchConfiguration> candidates = new ArrayList<ILaunchConfiguration>();
			for (ILaunchConfiguration candidate : launchConfigs) {
				if (candidate.getName().equals(name.get())) {
					candidates.add(candidate);
					break;
				}
			}
			if (candidates.size() > 0) launchConfigs = candidates.toArray(new ILaunchConfiguration[candidates.size()]);
			else launchConfigs = delegate.getMatchingLaunchConfigurations(launchSpec, launchConfigs);
		}
		catch (Exception e)	{
		}

		ILaunchConfiguration config = launchConfigs != null && launchConfigs.length > 0 ? launchConfigs[0] : null;
		try {
			if (config != null || !isDeleted.get()) {
				config = LaunchManager.getInstance().createOrUpdateLaunchConfiguration(config, launchSpec);
			}
		}
		catch (Exception e) {
		}

		return config;
	}
}
