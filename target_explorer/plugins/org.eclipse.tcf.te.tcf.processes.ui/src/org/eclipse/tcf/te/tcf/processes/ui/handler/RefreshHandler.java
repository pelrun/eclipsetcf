/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.handler;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.model.PendingOperationModelNode;
import org.eclipse.tcf.te.runtime.model.interfaces.IContainerModelNode;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx.QueryState;
import org.eclipse.tcf.te.runtime.model.interfaces.contexts.IAsyncRefreshableCtx.QueryType;
import org.eclipse.tcf.te.tcf.core.model.interfaces.IModel;
import org.eclipse.tcf.te.tcf.core.model.interfaces.services.IModelRefreshService;
import org.eclipse.tcf.te.tcf.processes.core.model.nodes.PendingOperationNode;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Refresh handler implementation.
 */
public class RefreshHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			List<IModelNode> nodes = ((IStructuredSelection) selection).toList();
			for (IModelNode node : nodes) {
				if (node instanceof IModel) {
					final IModel model = (IModel)node;
					Assert.isNotNull(model);

					// Get the asynchronous refresh context adapter
					final IAsyncRefreshableCtx refreshable = (IAsyncRefreshableCtx)model.getAdapter(IAsyncRefreshableCtx.class);
					if (refreshable != null) {
						// If refresh is already in progress -> drop out
						if (refreshable.getQueryState(QueryType.CHILD_LIST).equals(QueryState.IN_PROGRESS)) {
							return null;
						}

						// Set the query state to IN_PROGRESS
						refreshable.setQueryState(QueryType.CHILD_LIST, QueryState.IN_PROGRESS);
						// Create a new pending operation node and associate it with the refreshable
						PendingOperationModelNode pendingNode = new PendingOperationNode();
						pendingNode.setParent(model);
						refreshable.setPendingOperationNode(pendingNode);

						// Trigger a refresh of the view content.
						ChangeEvent changeEvent = new ChangeEvent(model, IContainerModelNode.NOTIFY_CHANGED, null, null);
						EventManager.getInstance().fireEvent(changeEvent);

						Runnable runnable = new Runnable() {
							@Override
							public void run() {
								// Don't send change events while refreshing
								final boolean changed = model.setChangeEventsEnabled(false);
								// Initiate the refresh of the model
								model.getService(IModelRefreshService.class).refresh(new Callback() {
									@Override
									protected void internalDone(Object caller, IStatus status) {
										// Mark the refresh as done
										refreshable.setQueryState(QueryType.CHILD_LIST, QueryState.DONE);
										// Reset the pending operation node
										refreshable.setPendingOperationNode(null);
										// Re-enable the change events if they had been enabled before
										if (changed) model.setChangeEventsEnabled(true);
										// Trigger a refresh of the view content.
										ChangeEvent event = new ChangeEvent(model, IContainerModelNode.NOTIFY_CHANGED, null, null);
										EventManager.getInstance().fireEvent(event);
									}
								});
							}
						};

						Protocol.invokeLater(runnable);
					}
				} else {
					final IModelNode finNode = node;

					Protocol.invokeLater(new Runnable() {
						@Override
						public void run() {
							final IModel model = finNode.getParent(IModel.class);
							Assert.isNotNull(model);
							model.getService(IModelRefreshService.class).refresh(finNode, null);
						}
					});
				}
			}
		}
		return null;
	}
}
