/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.interfaces.data;

import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.ui.interfaces.data.IDataExchangeNode;

/**
 * Public interface for wizard or dialog pages, panel, controls or other UI elements
 * exchanging data via a peer node object.
 */
public interface IPeerNodeDataExchangeNode extends IDataExchangeNode {

	/**
	 * Initialize the widgets based of the data from the given peer node.
	 * <p>
	 * This method may called multiple times during the lifetime of the node and the given
	 * peer node might be even <code>null</code>.
	 *
	 * @param data The peer node or <code>null</code>.
	 */
	public void setupData(IPeerNode peerNode);

	/**
	 * Extract the data from the widgets and write it back to the given peer node.
	 * <p>
	 * This method may called multiple times during the lifetime of the node and the given
	 * peer node might be even <code>null</code>.
	 *
	 * @param data The peer node or <code>null</code>.
	 */
	public void extractData(IPeerNode peerNode);
}
