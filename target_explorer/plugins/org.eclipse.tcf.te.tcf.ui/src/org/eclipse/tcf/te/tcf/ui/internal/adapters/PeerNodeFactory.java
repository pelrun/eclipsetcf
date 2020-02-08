/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal.adapters;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.ui.views.editor.EditorInput;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.internal.part.NullEditorInput;

/**
 * The element factory to create an peer model editor input from a memento which is read
 * from an external persistent storage and holds a peer id.
 */
@SuppressWarnings("restriction")
public class PeerNodeFactory implements IElementFactory {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IElementFactory#createElement(org.eclipse.ui.IMemento)
	 */
	@Override
	public IAdaptable createElement(IMemento memento) {
		final AtomicReference<IPeerNode> node = new AtomicReference<IPeerNode>();
		final String peerId = memento.getString("peerId"); //$NON-NLS-1$
		if (peerId != null) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					node.set(ModelManager.getPeerModel().getService(IPeerModelLookupService.class).lkupPeerModelById(peerId));
				}
			};

			Assert.isTrue(!Protocol.isDispatchThread());
			Protocol.invokeAndWait(runnable);

			// If the node is null, this might mean that the peer to restore is a dynamically discovered peer.
			// In this case, we have to wait a little bit to give the locator service the chance to sync.
			if (node.get() == null) {
				// Sleep shortly
				try { Thread.sleep(300); } catch (InterruptedException e) {}

				// Refresh and try again to query the node
				Runnable runnable2 = new Runnable() {
					@Override
					public void run() {
						ModelManager.getPeerModel().getService(IPeerModelRefreshService.class).refresh(null);
						node.set(ModelManager.getPeerModel().getService(IPeerModelLookupService.class).lkupPeerModelById(peerId));
					}
				};

				Protocol.invokeAndWait(runnable2);
			}
		}

		return node.get() != null ? new EditorInput(node.get()) : new NullEditorInput();
	}
}
