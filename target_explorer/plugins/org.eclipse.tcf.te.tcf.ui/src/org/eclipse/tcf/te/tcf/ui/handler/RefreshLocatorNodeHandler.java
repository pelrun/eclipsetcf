/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Locator node refresh handler implementation.
 */
public class RefreshLocatorNodeHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Assert.isTrue(!Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// Get the current selection
		ISelection selection = HandlerUtil.getCurrentSelection(event);

		// The selection must be a structured selection and must not be empty
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			// The list of locator model instances to refresh
			List<ILocatorNode> locatorNodesToRefresh = new ArrayList<ILocatorNode>();

			// Iterate the selection and determine the model instances
			Iterator<?> iterator = ((IStructuredSelection)selection).iterator();
			while (iterator.hasNext()) {
				Object element = iterator.next();
				if (element instanceof ILocatorNode) {
					ILocatorNode node = (ILocatorNode)element;

					if (!locatorNodesToRefresh.contains(node)) {
						locatorNodesToRefresh.add(node);
					}
				}
			}

			final ILocatorModel model = ModelManager.getLocatorModel();

			// Trigger an refresh on all determined models and wait for the
			// refresh to complete. Once completed, fire the parent callback.
			for (final ILocatorNode node : locatorNodesToRefresh) {
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						model.getService(ILocatorModelRefreshService.class).refresh(node, null);
					}
				};
				Protocol.invokeLater(runnable);
			}
		}

		return null;
	}
}
