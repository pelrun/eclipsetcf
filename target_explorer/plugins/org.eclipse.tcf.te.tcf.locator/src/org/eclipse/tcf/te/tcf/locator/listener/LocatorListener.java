/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.listener;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ILocator;
import org.eclipse.tcf.te.runtime.utils.net.IPAddressUtil;
import org.eclipse.tcf.te.tcf.locator.activator.CoreBundleActivator;
import org.eclipse.tcf.te.tcf.locator.interfaces.ITracing;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelUpdateService;

/**
 * Locator listener implementation.
 */
public final class LocatorListener implements ILocator.LocatorListener {
	// Reference to the parent model
	/* default */ final ILocatorModel model;

	/**
	 * Constructor.
	 *
	 * @param model The parent locator model. Must not be <code>null</code>.
	 */
	public LocatorListener(ILocatorModel model) {
		super();

		Assert.isNotNull(model);
		this.model = model;
	}

	/**
	 * Returns if or if not the given peer is filtered.
	 *
	 * @param peer The peer or <code>null</code>.
	 * @return <code>True</code> if the given peer is filtered, <code>false</code> otherwise.
	 */
	private boolean isFiltered(IPeer peer) {
		boolean filtered = peer == null;

		if (!filtered) {
			String value = peer.getAttributes().get("ValueAdd"); //$NON-NLS-1$
			boolean isValueAdd = value != null && ("1".equals(value.trim()) || Boolean.parseBoolean(value.trim())); //$NON-NLS-1$

			filtered |= isValueAdd;

			filtered |= peer.getName() != null
							&& (peer.getName().endsWith("Command Server") || peer.getName().endsWith("CLI Server")); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return filtered;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.ILocator.LocatorListener#peerAdded(org.eclipse.tcf.protocol.IPeer)
	 */
	@Override
	public void peerAdded(IPeer peer) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("LocatorListener.peerAdded( " + (peer != null ? peer.getID() : null) + " )", ITracing.ID_TRACE_LOCATOR_LISTENER, this); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (isFiltered(peer)) return;

		if (model != null && peer != null) {
			IPeer lkupPeer = model.getService(ILocatorModelLookupService.class).lkupPeerById(peer.getID());
			if (lkupPeer == null) {
				// Double check with "ClientID" if set
				String clientID = peer.getAttributes().get("ClientID"); //$NON-NLS-1$
				if (clientID != null) {
					lkupPeer = model.getService(ILocatorModelLookupService.class).lkupPeerById(clientID);
				}
			}
			if (lkupPeer == null) {
				// Validate peer before adding
				lkupPeer = model.validatePeer(peer);
				if (lkupPeer != null) model.getService(ILocatorModelUpdateService.class).add(lkupPeer);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.ILocator.LocatorListener#peerChanged(org.eclipse.tcf.protocol.IPeer)
	 */
	@Override
	public void peerChanged(IPeer peer) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("LocatorListener.peerChanged( " + (peer != null ? peer.getID() : null) + " )", ITracing.ID_TRACE_LOCATOR_LISTENER, this); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (isFiltered(peer)) return;

		if (model != null && peer != null) {
			// find the corresponding model node to remove
			IPeer lkupPeer = model.getService(ILocatorModelLookupService.class).lkupPeerById(peer.getID());
			if (lkupPeer == null) {
				// Double check with "ClientID" if set
				String clientID = peer.getAttributes().get("ClientID"); //$NON-NLS-1$
				if (clientID != null) {
					lkupPeer = model.getService(ILocatorModelLookupService.class).lkupPeerById(clientID);
				}
			}
			// Update the peer instance
			if (lkupPeer != null) {
				model.getService(ILocatorModelUpdateService.class).update(lkupPeer, peer);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.ILocator.LocatorListener#peerRemoved(java.lang.String)
	 */
	@Override
	public void peerRemoved(String id) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("LocatorListener.peerRemoved( " + id + " )", ITracing.ID_TRACE_LOCATOR_LISTENER, this); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (model != null && id != null) {
			// find the corresponding model node to remove
			IPeer lkupPeer = model.getService(ILocatorModelLookupService.class).lkupPeerById(id);

			// If we cannot find a model node, it is probably because the remove is sent for the
			// non-loopback addresses of the localhost. We have to double check this.
			if (lkupPeer == null) {
				int beginIndex = id.indexOf(':');
				int endIndex = id.lastIndexOf(':');
				String ip = id.substring(beginIndex+1, endIndex);

				// Get the loopback address
				String loopback = IPAddressUtil.getInstance().getIPv4LoopbackAddress();
				// Empty IP address means loopback
				if ("".equals(ip)) ip = loopback; //$NON-NLS-1$
				else {
					if (IPAddressUtil.getInstance().isLocalHost(ip)) {
						ip = loopback;
					}
				}
				// Build up the new id to lookup
				StringBuilder newId = new StringBuilder();
				newId.append(id.substring(0, beginIndex));
				newId.append(':');
				newId.append(ip);
				newId.append(':');
				newId.append(id.substring(endIndex + 1));

				// Try the lookup again
				lkupPeer = model.getService(ILocatorModelLookupService.class).lkupPeerById(newId.toString());
			}

			// If the model node is found in the model, process the removal.
			if (lkupPeer != null) {
				model.getService(ILocatorModelUpdateService.class).remove(lkupPeer);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.services.ILocator.LocatorListener#peerHeartBeat(java.lang.String)
	 */
	@Override
	public void peerHeartBeat(String id) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		if (CoreBundleActivator.getTraceHandler().isSlotEnabled(0, ITracing.ID_TRACE_LOCATOR_LISTENER)) {
			CoreBundleActivator.getTraceHandler().trace("LocatorListener.peerHeartBeat( " + id + " )", ITracing.ID_TRACE_LOCATOR_LISTENER, this); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

}
