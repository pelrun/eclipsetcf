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

import org.eclipse.tcf.protocol.IChannel.IChannelListener;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.core.interfaces.IDecoratable;
import org.eclipse.tcf.te.runtime.model.interfaces.IContainerModelNode;

/**
 * The peer model is an extension to the TCF peer representation, implementing the {@link IPeer}
 * interface. The peer model provides an offline cache for a peers known list of local and remote
 * services and is the merge point of peer attributes from custom data storages.
 * <p>
 * <b>Note:</b> Read and write access to the peer model must happen within the TCF dispatch thread.
 */
public interface IPeerNode extends IContainerModelNode, IDecoratable, IConnectable, IChannelListener {

	/**
	 * Delegate for
	 */
	public static interface IDelegate {

		/**
		 * Validate the peer node attributes.
		 * @param peerNode The peer node.
		 * @return <code>true</code> if the peer node is valid.
		 */
		public boolean isValid(IPeerNode peerNode);


		/**
		 * Check the visibility of this peer node.
		 * @param peerNode The peer node.
		 * @return <code>true</code> if the peer node is visible.
		 */
		public boolean isVisible(IPeerNode peerNode);
	}

	/**
	 * Returns the parent locator model instance.
	 * <p>
	 * This method may be called from any thread.
	 *
	 * @return The parent locator model instance.
	 */
	public IPeerModel getModel();

	/**
	 * Returns the native {@link IPeer} object.
	 * <p>
	 * This method may be called from any thread.
	 *
	 * @return The native {@link IPeer} instance.
	 */
	public IPeer getPeer();

	/**
	 * Returns the peer id.
	 * <p>
	 * This method may be called from any thread.
	 *
	 * @return The peer id.
	 */
	public String getPeerId();

	/**
	 * Returns the peer type.
	 * <p>
	 * This method may be called from any thread.
	 *
	 * @return The peer type or <code>null</code>.
	 */
	public String getPeerType();

	/**
	 * Returns if or if not the peer attributes are complete to open a channel to it.
	 *
	 * @return <code>True</code> if the peer attributes are complete, <code>false</code> otherwise.
	 */
	public boolean isValid();
}
