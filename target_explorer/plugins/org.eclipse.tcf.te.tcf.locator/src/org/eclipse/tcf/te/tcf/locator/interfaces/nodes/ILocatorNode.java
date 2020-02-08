/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.interfaces.nodes;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.core.interfaces.IDecoratable;
import org.eclipse.tcf.te.runtime.model.interfaces.IContainerModelNode;

/**
 * The locator node is an extension to the TCF peer representation.
 * <p>
 * <b>Note:</b> Read and write access to the peer model must happen within the TCF dispatch thread.
 */
public interface ILocatorNode extends IContainerModelNode, IDecoratable {

	public static final String PROPERTY_STATIC_INSTANCE = "staticInstance"; //$NON-NLS-1$

	/**
	 * Returns the native {@link IPeer} object.
	 * <p>
	 * This method may be called from any thread.
	 *
	 * @return The native {@link IPeer} instance.
	 */
	public IPeer getPeer();

	/**
	 * Returns the list of child peers this peer is a proxy for.
	 *
	 * @return The list of child peers or an empty list.
	 */
	public IPeer[] getPeers();

	/**
	 * Returns <code>true</code> if this node was manually added.
	 * @return <code>true</code> for manually added nodes.
	 */
	public boolean isStatic();

	/**
	 * Returns true, if a peer was already discovered for a static node.
	 */
	public boolean isDiscovered();
}
