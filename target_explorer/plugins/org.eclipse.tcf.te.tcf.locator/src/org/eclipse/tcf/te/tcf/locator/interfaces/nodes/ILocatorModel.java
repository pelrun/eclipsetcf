/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces.nodes;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.services.ILocator;
import org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelService;


/**
 * The locator model is an extension to the TCF locator service. The
 * model allows to store additional properties for each peer, keep
 * track of peers from different origins.
 * <p>
 * <b>Note:</b> Updates to the locator model, and the locator model
 * children needs to be performed in the TCF dispatch thread. The
 * locator model and all child model nodes do assert this core
 * assumption. To maintain consistency, and to avoid any performance
 * overhead for thread synchronization, the model read access must
 * happen in the TCF dispatch thread as well.
 *
 * @see ILocator
 */
public interface ILocatorModel extends IAdaptable {

	/**
	 * Adds the specified listener to the list of model listener.
	 * If the same listener has been added before, the listener will
	 * not be added again.
	 *
	 * @param listener The listener. Must not be <code>null</code>.
	 */
	public void addListener(ILocatorModelListener listener);

	/**
	 * Removes the specified listener from the list of model listener.
	 *
	 * @param listener The listener. Must not be <code>null</code>.
	 */
	public void removeListener(ILocatorModelListener listener);

	/**
	 * Returns the list of registered model listeners.
	 *
	 * @return The list of registered model listeners or an empty list.
	 */
	public ILocatorModelListener[] getListener();

	/**
	 * Dispose the locator model instance.
	 */
	public void dispose();

	/**
	 * Returns if or if not the locator model instance is disposed.
	 *
	 * @return <code>True</code> if the locator model instance is disposed, <code>false/code> otherwise.
	 */
	public boolean isDisposed();

	/**
	 * Returns the list of locator nodes.
	 *
	 * @return The list of locator nodes or an empty list.
	 */
	public ILocatorNode[] getLocatorNodes();

	/**
	 * Returns the list of known peers.
	 *
	 * @return The list of known peers or an empty list.
	 */
	public IPeer[] getPeers();

	/**
	 * Returns the locator model service, implementing at least the specified
	 * service interface.
	 *
	 * @param serviceInterface The service interface class. Must not be <code>null</code>.
	 * @return The service instance implementing the specified service interface, or <code>null</code>.
	 */
	public <V extends ILocatorModelService> V getService(Class<V> serviceInterface);

	/**
	 * Validate the given peer.
	 * <p>
	 * If the peer is for local host, the peer is valid only if there is
	 * no other local host peer known to the model using the loopback or
	 * the canonical host address.
	 *
	 * @param peer The peer. Must not be <code>null</code>.
	 * @return The peer if the peer is valid, or <code>null</code> if not.
	 */
	public IPeer validatePeer(IPeer peer);
}
