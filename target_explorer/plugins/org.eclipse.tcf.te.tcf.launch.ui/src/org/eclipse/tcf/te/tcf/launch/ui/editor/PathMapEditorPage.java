/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IPathMap;
import org.eclipse.tcf.services.IPathMap.PathMapRule;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.statushandler.StatusHandlerManager;
import org.eclipse.tcf.te.runtime.statushandler.interfaces.IStatusHandler;
import org.eclipse.tcf.te.runtime.statushandler.interfaces.IStatusHandlerConstants;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.interfaces.IPathMapService;
import org.eclipse.tcf.te.tcf.launch.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.launch.ui.editor.tabs.PathMapTab;
import org.eclipse.tcf.te.tcf.launch.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.launch.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;

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

		final IPeerModel peerModel = getPeerModel(getEditorInput());
		if (peerModel != null && peerModel.getPeer() != null) {
			final IChannel channel = Tcf.getChannelManager().getChannel(peerModel.getPeer());
			if (channel != null && IChannel.STATE_OPEN == channel.getState()) {
				// Channel is open -> Have to update the path maps
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						final IPeer peer = peerModel.getPeer();
						final IPathMapService service = ServiceManager.getInstance().getService(peer, IPathMapService.class);
						final IPathMap svc = channel.getRemoteService(IPathMap.class);
						if (service != null && svc != null) {
							final PathMapRule[] map = service.getPathMap(peer);
							if (map != null && map.length > 0) {
								svc.set(map, new IPathMap.DoneSet() {
									@Override
									public void doneSet(IToken token, Exception error) {
										if (error != null) {
											IStatus status = new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(),
																		NLS.bind(Messages.PathMapEditorPage_error_apply, peerModel.getName(), error.getLocalizedMessage()),
																		error);
											IStatusHandler[] handlers = StatusHandlerManager.getInstance().getHandler(peerModel);
											if (handlers.length > 0) {
												IPropertiesContainer data = new PropertiesContainer();
												data.setProperty(IStatusHandlerConstants.PROPERTY_TITLE, Messages.PathMapEditorPage_error_title);
												data.setProperty(IStatusHandlerConstants.PROPERTY_CONTEXT_HELP_ID, IContextHelpIds.MESSAGE_APPLY_PATHMAP_FAILED);
												data.setProperty(IStatusHandlerConstants.PROPERTY_CALLER, this);

												handlers[0].handleStatus(status, data, null);
											} else {
												UIPlugin.getDefault().getLog().log(status);
											}
										}
									}
								});
							}
						}

					}
				};

				Protocol.invokeLater(runnable);
			}
		}
	}
}
