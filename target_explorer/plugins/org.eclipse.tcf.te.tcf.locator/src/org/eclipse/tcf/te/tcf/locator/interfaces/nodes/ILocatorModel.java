/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces.nodes;

import java.util.List;

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
	 * Returns the list of known peers.
	 *
	 * @return The list of known peers or an empty list.
	 */
	public IPeer[] getPeers();

	/**
	 * Returns an unmodifiable list of known children for the given parent peer.
	 *
	 * @param parentPeerID The parent peer id. Must not be <code>null</code>.
	 * @return The child list.
	 */
	public List<IPeer> getChildren(String parentPeerID);

	/**
	 * Sets the list of known children for the given parent peer.
	 *
	 * @param parentPeerID The parent peer id. Must not be <code>null</code>.
	 * @param children The list of children or <code>null</code> to remove the parent peer.
	 */
	public void setChildren(String parentPeerID, List<IPeer> children);

	/**
	 * Returns the locator model service, implementing at least the specified
	 * service interface.
	 *
	 * @param serviceInterface The service interface class. Must not be <code>null</code>.
	 * @return The service instance implementing the specified service interface, or <code>null</code>.
	 */
	public <V extends ILocatorModelService> V getService(Class<V> serviceInterface);
}
