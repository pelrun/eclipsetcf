/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.terminals.ui.launcher;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tm.terminal.view.ui.interfaces.IMementoHandler;
import org.eclipse.ui.IMemento;

/**
 * Terminals (TCF) terminal connection memento handler implementation.
 */
public class TerminalsMementoHandler implements IMementoHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.IMementoHandler#saveState(org.eclipse.ui.IMemento, java.util.Map)
	 */
	@Override
	public void saveState(IMemento memento, Map<String, Object> properties) {
		Assert.isNotNull(memento);
		Assert.isNotNull(properties);

		// Do not write the terminal title to the memento -> needs to
		// be recreated at the time of restoration.
		memento.putString(ITerminalsConnectorConstants.PROP_ENCODING, (String)properties.get(ITerminalsConnectorConstants.PROP_ENCODING));

		// Get the selection from the properties
		ISelection selection = (ISelection)properties.get(ITerminalsConnectorConstants.PROP_SELECTION);
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object element = ((IStructuredSelection)selection).getFirstElement();
			if (element instanceof IPeerNode) {
				IPeerNode peerNode = (IPeerNode)element;
				memento.putString("peerID", peerNode.getPeerId()); //$NON-NLS-1$
			}
		}
	}


	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.terminals.interfaces.IMementoHandler#restoreState(org.eclipse.ui.IMemento, java.util.Map)
	 */
	@Override
	public void restoreState(IMemento memento, Map<String, Object> properties) {
		Assert.isNotNull(memento);
		Assert.isNotNull(properties);

		// Restore the terminal properties from the memento
		properties.put(ITerminalsConnectorConstants.PROP_ENCODING, memento.getString(ITerminalsConnectorConstants.PROP_ENCODING));

		final String peerID = memento.getString("peerID"); //$NON-NLS-1$
		if (peerID != null) {
			final IPeerModel model = ModelManager.getPeerModel();
			Assert.isNotNull(model);
			final AtomicReference<IPeerNode> peerNode = new AtomicReference<IPeerNode>();
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					peerNode.set(model.getService(IPeerModelLookupService.class).lkupPeerModelById(peerID));
				}
			};
			Assert.isTrue(!Protocol.isDispatchThread());
			Protocol.invokeAndWait(runnable);

			// If the node is null, this might mean that the peer to restore is a dynamically discovered peer.
			// In this case, we have to wait a little bit to give the locator service the chance to sync.
			if (peerNode.get() == null) {
				// Sleep shortly
				try { Thread.sleep(300); } catch (InterruptedException e) {}
				Protocol.invokeAndWait(runnable);
			}

			if (peerNode.get() != null) {
				properties.put(ITerminalsConnectorConstants.PROP_SELECTION, new StructuredSelection(peerNode.get()));
			}
		}
	}
}
