/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces;

import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Interface for clients to implement that wishes to listen to changes to the peer model.
 */
public interface IPeerModelListener {

	/**
	 * Invoked if a peer node is added or removed to/from the peer model.
	 *
	 * @param model The changed locator model.
	 * @param peerNode The added/removed peer node.
	 * @param added <code>True</code> if the peer node got added, <code>false</code> if it got removed.
	 */
	public void modelChanged(IPeerModel model, IPeerNode peerNode, boolean added);

	/**
	 * Invoked if the peer model is disposed.
	 *
	 * @param model The disposed peer model.
	 */
	public void modelDisposed(IPeerModel model);
}
