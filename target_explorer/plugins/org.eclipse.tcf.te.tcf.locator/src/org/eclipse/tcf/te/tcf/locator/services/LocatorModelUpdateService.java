/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.services;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelUpdateService;


/**
 * Default locator model update service implementation.
 */
public class LocatorModelUpdateService extends AbstractLocatorModelService implements ILocatorModelUpdateService {

	/**
	 * Constructor.
	 *
	 * @param parentModel The parent locator model instance. Must not be <code>null</code>.
	 */
	public LocatorModelUpdateService(ILocatorModel parentModel) {
		super(parentModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelUpdateService#add(org.eclipse.tcf.protocol.IPeer)
	 */
	@Override
	public void add(final IPeer peer) {
		Assert.isNotNull(peer);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		Map<String, IPeer> peers = (Map<String, IPeer>)getLocatorModel().getAdapter(Map.class);
		Assert.isNotNull(peers);
		peers.put(peer.getID(), peer);

		final ILocatorModelListener[] listeners = getLocatorModel().getListener();
		if (listeners.length > 0) {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					for (ILocatorModelListener listener : listeners) {
						listener.modelChanged(getLocatorModel(), peer, true);
					}
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelUpdateService#remove(org.eclipse.tcf.protocol.IPeer)
	 */
	@Override
	public void remove(final IPeer peer) {
		Assert.isNotNull(peer);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		Map<String, IPeer> peers = (Map<String, IPeer>)getLocatorModel().getAdapter(Map.class);
		Assert.isNotNull(peers);
		peers.remove(peer.getID());

		final ILocatorModelListener[] listeners = getLocatorModel().getListener();
		if (listeners.length > 0) {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					for (ILocatorModelListener listener : listeners) {
						listener.modelChanged(getLocatorModel(), peer, false);
					}
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelUpdateService#update(org.eclipse.tcf.protocol.IPeer, org.eclipse.tcf.protocol.IPeer)
	 */
	@Override
	public void update(final IPeer oldPeer, final IPeer newPeer) {
		Assert.isNotNull(oldPeer);
		Assert.isNotNull(newPeer);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		Map<String, IPeer> peers = (Map<String, IPeer>)getLocatorModel().getAdapter(Map.class);
		Assert.isNotNull(peers);
		peers.remove(oldPeer.getID());
		peers.put(newPeer.getID(), newPeer);


		final ILocatorModelListener[] listeners = getLocatorModel().getListener();
		if (listeners.length > 0) {
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					for (ILocatorModelListener listener : listeners) {
						if (!oldPeer.getID().equals(newPeer.getID())) {
							listener.modelChanged(getLocatorModel(), oldPeer, false);
							listener.modelChanged(getLocatorModel(), newPeer, true);
						}
						else {
							listener.modelChanged(getLocatorModel(), newPeer, false);
						}
					}
				}
			});
		}
	}
}
