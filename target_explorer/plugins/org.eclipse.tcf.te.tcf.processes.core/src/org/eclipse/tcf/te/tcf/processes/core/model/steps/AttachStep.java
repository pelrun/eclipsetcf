/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.steps;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.services.IProcesses.ProcessContext;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.properties.PropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IDebugService;
import org.eclipse.tcf.te.runtime.statushandler.StatusHandlerManager;
import org.eclipse.tcf.te.runtime.statushandler.interfaces.IStatusHandler;
import org.eclipse.tcf.te.runtime.statushandler.interfaces.IStatusHandlerConstants;
import org.eclipse.tcf.te.tcf.core.Tcf;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.model.interfaces.IModel;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.processes.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IContextHelpIds;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.nls.Messages;

/**
 * Process attach step implementation.
 */
public class AttachStep {

	/**
	 * Attach to the given process context.
	 * <p>
	 * <b>Note:</b> This method must be called from within the TCF dispatch thread.
	 *
	 * @param node The context. Must not be <code>null</code>.
	 * @param callback The callback to invoke once the operation completed, or<code>null</code>.
	 */
	public void executeAttach(final IProcessContextNode node, final ICallback callback) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(node);

		// If the context is already attached, there is nothing to do
		if (node.getProcessContext() != null && !node.getProcessContext().isAttached()) {
			IPeerModel peerNode = (IPeerModel)node.getAdapter(IPeerModel.class);
			if (peerNode != null) {
				// Determine the debug service to attach to the peer node
				IDebugService dbgService = ServiceManager.getInstance().getService(peerNode, IDebugService.class, false);
				if (dbgService != null) {
					// Attach to the peer node first
					dbgService.attach(peerNode, new PropertiesContainer(), null, new Callback() {
						@Override
						protected void internalDone(Object caller, IStatus status) {
							callback.setProperty("launch", getProperty("launch")); //$NON-NLS-1$ //$NON-NLS-2$
							Runnable runnable = new Runnable() {
								@Override
								public void run() {
									doAttach(node, callback);
								}
							};
							if (Protocol.isDispatchThread()) runnable.run();
							else Protocol.invokeLater(runnable);
						}
					});
				} else {
					doAttach(node, callback);
				}
			} else {
				onError(node, Messages.AttachStep_error_connect, null, callback);
			}
		} else {
			if (node.getProcessContext() == null) {
				onError(node, Messages.AttachStep_error_connect, null, callback);
			} else {
				onDone(callback);
			}
		}
	}

	/**
	 * Opens a channel and perform the attach to the given context node.
	 * <p>
	 * <b>Note:</b> This method must be called from within the TCF dispatch thread.
	 *
	 * @param node The context node. Must not be <code>null</code>.
	 * @param callback The callback to invoke once the operation completed, or<code>null</code>.
	 */
	protected void doAttach(final IProcessContextNode node, final ICallback callback) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(node);

		// Determine the peer model node
		final IPeerModel peerNode = (IPeerModel)node.getAdapter(IPeerModel.class);

		// Open a channel
		Tcf.getChannelManager().openChannel(peerNode.getPeer(), null, new IChannelManager.DoneOpenChannel() {
			@Override
			public void doneOpenChannel(final Throwable error, final IChannel channel) {
				if (error == null) {
					final IProcesses service = channel.getRemoteService(IProcesses.class);
					if (service != null) {
						service.getContext(node.getStringProperty(IModelNode.PROPERTY_ID), new IProcesses.DoneGetContext() {
							@Override
							public void doneGetContext(IToken token, Exception error, ProcessContext context) {
								if (error == null && context != null) {
									context.attach(new IProcesses.DoneCommand() {
										@Override
										public void doneCommand(IToken token, Exception error) {
											if (error == null) {
												// We are attached now, trigger a refresh of the node
												IModel model = node.getParent(IModel.class);
												Assert.isNotNull(model);
												model.getService(IModelRefreshService.class).refresh(node, new Callback() {
													@Override
                                                    protected void internalDone(Object caller, IStatus status) {
														onDone(callback);
													}
												});
											} else {
												onError(node, Messages.AttachStep_error_attach, error, callback);
											}
										}
									});
								} else {
									onError(node, Messages.AttachStep_error_getContext, error, callback);
								}
							}
						});
					} else {
						onError(node, Messages.AttachStep_error_connect, null, callback);
					}
				} else {
					onError(node, Messages.AttachStep_error_openChannel, error, callback);
				}
			}
		});
	}

	/**
	 * Error handler. Called if a step failed.
	 *
	 * @param channel The channel or <code>null</code>.
	 * @param context The status handler context. Must not be <code>null</code>:
	 * @param message The message or <code>null</code>.
	 * @param error The error or <code>null</code>.
	 * @param callback The callback or <code>null</code>.
	 */
	protected void onError(Object context, String message, Throwable error, ICallback callback) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		String detailMessage = error != null ? error.getMessage() : null;
		if (detailMessage != null && detailMessage.contains("\n")) { //$NON-NLS-1$
			detailMessage = detailMessage.replaceAll("\n", ", "); //$NON-NLS-1$ //$NON-NLS-2$
			detailMessage = detailMessage.replaceAll(":, ", ": "); //$NON-NLS-1$ //$NON-NLS-2$
		}

		String fullMessage = message;
		if (fullMessage != null) fullMessage = NLS.bind(fullMessage, detailMessage != null ? detailMessage : ""); //$NON-NLS-1$
		else fullMessage = detailMessage;

		if (fullMessage != null) {
			IStatus status = new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(), fullMessage, error);

			if (callback == null) {
				IStatusHandler[] handlers = StatusHandlerManager.getInstance().getHandler(context);
				if (handlers.length > 0) {
					IPropertiesContainer data = new PropertiesContainer();
					data.setProperty(IStatusHandlerConstants.PROPERTY_TITLE, Messages.AttachStep_error_title);
					data.setProperty(IStatusHandlerConstants.PROPERTY_CONTEXT_HELP_ID, IContextHelpIds.MESSAGE_ATTACH_FAILED);
					data.setProperty(IStatusHandlerConstants.PROPERTY_CALLER, this);

					handlers[0].handleStatus(status, data, null);
				} else {
					CoreBundleActivator.getDefault().getLog().log(status);
				}
			}
			else {
				callback.done(this, status);
			}
		}
	}

	/**
	 * Done handler. Called if all necessary steps are completed.
	 *
	 * @param callback The callback to invoke or <code>null</code>
	 */
	protected void onDone(ICallback callback) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		if (callback != null) callback.done(this, Status.OK_STATUS);
	}
}
