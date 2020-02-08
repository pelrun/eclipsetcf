/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.handler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.AsyncCallbackCollector;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.tcf.core.async.CallbackInvocationDelegate;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService;
import org.eclipse.tcf.te.ui.views.ViewsUtil;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Refresh handler implementation.
 */
public class RefreshPeerNodeHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Get the current selection
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		// Refresh the selection
		if (selection != null) refresh(selection, new Callback() {
			@Override
			protected void internalDone(Object caller, IStatus status) {
				// Refresh the view
				ViewsUtil.refresh(IUIConstants.ID_EXPLORER);
			}
		});

		return null;
	}

	/**
	 * Refresh the elements of the given selection.
	 *
	 * @param selection The selection. Must not be <code>null</code>.
	 * @param callback The callback. Must not be <code>null</code>:
	 */
	public void refresh(final ISelection selection, final ICallback callback) {
		Assert.isTrue(!Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(selection);
		Assert.isNotNull(callback);

		// The selection must be a structured selection and must not be empty
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			// The list of locator model instances to refresh
			List<IPeerModel> locatorToRefresh = new ArrayList<IPeerModel>();
			List<IPeerNode> peerToRefresh = new ArrayList<IPeerNode>();

			// Iterate the selection and determine the model instances
			Iterator<?> iterator = ((IStructuredSelection)selection).iterator();
			while (iterator.hasNext()) {
				Object element = iterator.next();
				Assert.isTrue(element instanceof IPeerNode);
				IPeerNode node = (IPeerNode)element;

				// Get the associated locator model of the node
				IPeerModel model = (IPeerModel)node.getAdapter(IPeerModel.class);
				Assert.isNotNull(model);
				// If not yet in the list, add it
				if (!locatorToRefresh.contains(model)) {
					locatorToRefresh.add(model);
				}
				if (!peerToRefresh.contains(node)) {
					peerToRefresh.add(node);
				}
			}

			// Trigger an refresh on all determined models and wait for the
			// refresh to complete. Once completed, fire the parent callback.
			AsyncCallbackCollector collector = new AsyncCallbackCollector(callback, new CallbackInvocationDelegate());
			for (IPeerModel model : locatorToRefresh) {
				final IPeerModel finModel = model;
				final ICallback innerCallback = new AsyncCallbackCollector.SimpleCollectorCallback(collector);

				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						finModel.getService(IPeerModelRefreshService.class).refresh(new Callback() {
							@Override
							protected void internalDone(Object caller, IStatus status) {
								innerCallback.done(this, Status.OK_STATUS);
							}
						});
					}
				};
				Protocol.invokeLater(runnable);
			}

			// Mark the collector as fully initialized
			collector.initDone();
		} else {
			callback.done(this, Status.OK_STATUS);
		}
	}
}
