/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.persistence;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.persistence.delegates.GsonMapPersistenceDelegate;
import org.eclipse.tcf.te.tcf.core.peers.Peer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.locator.nodes.PeerNode;

/**
 * Peer to string persistence delegate implementation.
 */
public class GsonPeerPersistenceDelegate extends GsonMapPersistenceDelegate {

	/**
	 * Constructor.
	 */
	public GsonPeerPersistenceDelegate() {
		super("peer"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate#getPersistedClass(java.lang.Object)
	 */
	@Override
	public Class<?> getPersistedClass(Object context) {
		return IPeer.class;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.AbstractPropertiesPersistenceDelegate#toMap(java.lang.Object)
	 */
	@Override
	protected Map<String, Object> toMap(final Object context) throws IOException {
		IPeer peer = getPeer(context);
		if (peer != null) {
			return super.toMap(peer.getAttributes());
		}

		return new HashMap<String, Object>();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.AbstractPropertiesPersistenceDelegate#fromMap(java.util.Map, java.lang.Object)
	 */
	@Override
	protected Object fromMap(Map<String, Object> map, Object context) throws IOException {
		Map<String,String> attrs = new HashMap<String,String>();
		for (Entry<String, Object> entry : map.entrySet()) {
			if (entry != null)
				attrs.put(entry.getKey(), entry.getValue().toString());
		}

		final IPeer peer = new Peer(attrs);

		if (context instanceof IPeer || IPeer.class.equals(context)) {
			return peer;
		}
		else if (context instanceof Class && (((Class<?>)context).isAssignableFrom(IPeerNode.class))) {
			final AtomicReference<IPeerNode> model = new AtomicReference<IPeerNode>();

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					// Get the id of the decoded attributes
					String id = peer.getID();
					if (id != null) {
						// Lookup the id within the model
						IPeerNode peerNode = ModelManager.getPeerModel().getService(IPeerModelLookupService.class).lkupPeerModelById(id);
						if (peerNode == null) {
							// Not found in the model -> create a ghost object
							peerNode = new PeerNode(ModelManager.getPeerModel(), peer);
							peerNode.setProperty(IModelNode.PROPERTY_IS_GHOST, true);
						}

						model.set(peerNode);
					}
				}
			};

			if (Protocol.isDispatchThread()) {
				runnable.run();
			}
			else {
				Protocol.invokeAndWait(runnable);
			}

			return model.get();
		}

		return null;
	}

	/**
	 * Get a peer from the given context.
	 *
	 * @param context The context. Must not be <code>null</code>.
	 * @return The peer or <code>null</code>.
	 */
	protected IPeer getPeer(Object context) {
		IPeer peer = null;

		if (context instanceof IPeer) {
			peer = (IPeer)context;
		}
		else if (context instanceof IPeerNode) {
			peer = ((IPeerNode)context).getPeer();
		}
		else if (context instanceof IPeerNodeProvider) {
			peer = ((IPeerNodeProvider)context).getPeerNode().getPeer();
		}

		return peer;
	}
}
