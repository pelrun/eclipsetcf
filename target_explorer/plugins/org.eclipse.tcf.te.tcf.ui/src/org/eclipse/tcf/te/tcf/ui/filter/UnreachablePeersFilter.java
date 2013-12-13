/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.filter;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;

/**
 * Filter implementation filtering unreachable peers.
 */
public class UnreachablePeersFilter extends ViewerFilter {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {

		// Filter only elements of simulator IPeerNode
		if (element instanceof IPeerNode) {
			final IPeerNode peerNode = (IPeerNode)element;

			// Determine the current action of the peer model
			final int[] state = new int[1];
			if (Protocol.isDispatchThread()) {
				state[0] = peerNode.getIntProperty(IPeerNodeProperties.PROP_STATE);
			} else {
				Protocol.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						state[0] = peerNode.getIntProperty(IPeerNodeProperties.PROP_STATE);
					}
				});
			}

			return state[0] != IPeerNodeProperties.STATE_NOT_REACHABLE && state[0] != IPeerNodeProperties.STATE_ERROR;
		}

		return true;
	}

}
