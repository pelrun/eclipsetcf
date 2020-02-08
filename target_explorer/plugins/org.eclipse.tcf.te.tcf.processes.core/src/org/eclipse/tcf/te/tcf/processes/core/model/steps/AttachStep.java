/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.core.model.steps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IChannel;
import org.eclipse.tcf.protocol.IToken;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.services.IProcesses.ProcessContext;
import org.eclipse.tcf.te.runtime.callback.AsyncCallbackCollector;
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
import org.eclipse.tcf.te.tcf.core.async.CallbackInvocationDelegate;
import org.eclipse.tcf.te.tcf.core.interfaces.IChannelManager;
import org.eclipse.tcf.te.tcf.core.model.interfaces.IModel;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.core.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.processes.core.interfaces.IContextHelpIds;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.nls.Messages;

/**
 * Process attach step implementation.
 */
public class AttachStep {

	/**
	 * Attach to the given list of process context nodes of the given peer model node.
	 * <p>
	 * <b>Note:</b> This method must be called from within the TCF dispatch thread.
	 *
	 * @param peerNode The peer model. Must not be <code>null</code>.
	 * @param nodes The list of process context nodes. Must not be <code>null</code>.
	 * @param callback The callback to invoke once the operation completed, or<code>null</code>.
	 */
	public void executeAttach(final IPeerNode peerNode, final IProcessContextNode[] nodes, final ICallback callback) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(peerNode);
		Assert.isNotNull(nodes);

		// Determine if we have to execute the attach at all
		final List<IProcessContextNode> nodesToAttach = new ArrayList<IProcessContextNode>();
		for (IProcessContextNode node : nodes) {
			IPeerNode parentPeerModel = (IPeerNode)node.getAdapter(IPeerNode.class);
			if (!peerNode.equals(parentPeerModel)) continue;

			// If not yet attached, we have to attach to it
			if (node.getProcessContext() != null && !node.getProcessContext().isAttached()) {
				if (!nodesToAttach.contains(node)) nodesToAttach.add(node);
			}
		}

		// Anything to attach?
		if (!nodesToAttach.isEmpty()) {
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
								doAttach(peerNode, Collections.unmodifiableList(nodesToAttach), callback);
							}
						};
						if (Protocol.isDispatchThread()) runnable.run();
						else Protocol.invokeLater(runnable);
					}
				});
			} else {
				doAttach(peerNode, Collections.unmodifiableList(nodesToAttach), callback);
			}
		} else {
			onDone(callback);
		}

	}

	/**
	 * Opens a channel and perform the attach to the given process context nodes.
	 * <p>
	 * <b>Note:</b> This method must be called from within the TCF dispatch thread.
	 *
	 * @param peerNode The peer model. Must not be <code>null</code>.
	 * @param nodes The process context node. Must not be <code>null</code>.
	 * @param callback The callback to invoke once the operation completed, or<code>null</code>.
	 */
	protected void doAttach(final IPeerNode peerNode, final List<IProcessContextNode> nodes, final ICallback callback) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(peerNode);
		Assert.isNotNull(nodes);

		// Loop the nodes and attach to them
		if (!nodes.isEmpty()) {
			// Open a channel
			Tcf.getChannelManager().openChannel(peerNode.getPeer(), null, new IChannelManager.DoneOpenChannel() {
				@Override
				public void doneOpenChannel(final Throwable error, final IChannel channel) {
					if (error == null) {
						final IProcesses service = channel.getRemoteService(IProcesses.class);
						if (service != null) {
							// Create the callback collector
							AsyncCallbackCollector collector = new AsyncCallbackCollector(new Callback() {
								@Override
								protected void internalDone(Object caller, IStatus status) {
									if (status.getSeverity() == IStatus.ERROR) {
										onError(peerNode, status.getMessage(), status.getException(), callback);
									} else {
										onDone(callback);
									}
								}
							}, new CallbackInvocationDelegate());

							for (final IProcessContextNode node: nodes) {
								final ICallback callback2 = new AsyncCallbackCollector.SimpleCollectorCallback(collector);
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
																callback2.done(AttachStep.this, Status.OK_STATUS);
															}
														});
													} else {
														IStatus status = new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(),
																		NLS.bind(Messages.AttachStep_error_attach, node.getName()), error);
														callback2.done(AttachStep.this, status);
													}
												}
											});
										} else {
											IStatus status = new Status(IStatus.ERROR, CoreBundleActivator.getUniqueIdentifier(),
																		NLS.bind(Messages.AttachStep_error_getContext, node.getName()), error);
											callback2.done(AttachStep.this, status);
										}
									}
								});
							}

							// Mark the collector initialization done
							collector.initDone();
						} else {
							onError(peerNode, NLS.bind(Messages.AttachStep_error_missingService, peerNode.getName()), null, callback);
						}
					} else {
						onError(peerNode, NLS.bind(Messages.AttachStep_error_openChannel, peerNode.getName()), error, callback);
					}
				}
			});
		} else {
			onDone(callback);
		}
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
		if (fullMessage != null && detailMessage != null) {
			fullMessage += NLS.bind(Messages.AttachStep_error_possibleCause, detailMessage);
		}
		else if (fullMessage == null) {
			fullMessage = detailMessage;
		}

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
