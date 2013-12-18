/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;

/**
 * Interface for clients to implement that wishes to listen to changes to the locator model.
 */
public interface ILocatorModelListener {

	/**
	 * Invoked if a peer is added or removed to/from the locator model.
	 *
	 * @param model The changed locator model.
	 * @param peer The added/removed peer.
	 * @param added <code>True</code> if the peer got added, <code>false</code> if it got removed.
	 */
	public void modelChanged(ILocatorModel model, IPeer peer, boolean added);

	/**
	 * Invoked if the locator model is disposed.
	 *
	 * @param model The disposed locator model.
	 */
	public void modelDisposed(ILocatorModel model);
}
