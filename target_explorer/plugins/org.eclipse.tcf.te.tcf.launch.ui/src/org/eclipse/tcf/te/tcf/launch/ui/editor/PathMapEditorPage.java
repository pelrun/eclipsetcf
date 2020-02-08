/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.editor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.statushandler.StatusHandlerManager;
import org.eclipse.tcf.te.runtime.statushandler.interfaces.IStatusHandler;
import org.eclipse.tcf.te.runtime.statushandler.interfaces.IStatusHandlerConstants;
import org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService;
import org.eclipse.tcf.te.tcf.launch.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.launch.ui.editor.tabs.PathMapTab;
import org.eclipse.tcf.te.tcf.launch.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.launch.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * TCF path map launch configuration tab container page implementation.
 */
public class PathMapEditorPage extends AbstractTcfLaunchTabContainerEditorPage {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.editor.AbstractLaunchTabContainerEditorPage#createLaunchConfigurationTab()
	 */
	@Override
	protected AbstractLaunchConfigurationTab createLaunchConfigurationTab() {
		return new PathMapTab(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.launch.ui.editor.AbstractTcfLaunchTabContainerEditorPage#onPostSave(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	@Override
	protected void onPostSave(ILaunchConfiguration config) {
		Assert.isNotNull(config);

		final IPeerNode peerNode = getPeerModel(getEditorInput());
		if (peerNode != null && peerNode.getPeer() != null) {
			IPathMapService service = ServiceManager.getInstance().getService(peerNode.getPeer(), IPathMapService.class);
			if (service != null) {
				service.applyPathMap(peerNode.getPeer(), false, true, new Callback() {
					@Override
					protected void internalDone(Object caller, IStatus status) {
						if (status != null && status.getSeverity() == IStatus.ERROR) {
							IStatus status2 = new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(),
														 NLS.bind(Messages.PathMapEditorPage_error_apply, peerNode.getName(), status.getMessage()),
														 status.getException());
							IStatusHandler[] handlers = StatusHandlerManager.getInstance().getHandler(peerNode);
							if (handlers.length > 0) {
								IPropertiesContainer data = new PropertiesContainer();
								data.setProperty(IStatusHandlerConstants.PROPERTY_TITLE, Messages.PathMapEditorPage_error_title);
								data.setProperty(IStatusHandlerConstants.PROPERTY_CONTEXT_HELP_ID, IContextHelpIds.MESSAGE_APPLY_PATHMAP_FAILED);
								data.setProperty(IStatusHandlerConstants.PROPERTY_CALLER, this);

								handlers[0].handleStatus(status2, data, null);
							} else {
								UIPlugin.getDefault().getLog().log(status2);
							}
						}
					}
				});
			}
		}
	}
}
