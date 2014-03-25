/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.locator.delegates;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.nodes.interfaces.wire.IWireTypeNetwork;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.utils.SimulatorUtils;

/**
 * PeerNodeValidationDelegate
 */
public class PeerNodeValidationDelegate implements IPeerNode.IDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeValidationDelegate#isValid(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode)
	 */
	@Override
	public boolean isValid(final IPeerNode peerNode) {
		final AtomicBoolean valid = new AtomicBoolean(true);

		Protocol.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				// Determine the transport method
				String transport = peerNode.getPeer().getTransportName();
				// If the transport is not set, the peer attributes are incomplete
				if (transport == null) {
					valid.set(false);
				} else {
					// For TCP or SSL transport, ATTR_IP_HOST must not be null.
					String ip = peerNode.getPeer().getAttributes().get(IPeer.ATTR_IP_HOST);
					String port = peerNode.getPeer().getAttributes().get(IPeer.ATTR_IP_PORT);
					String autoPortString = peerNode.getPeer().getAttributes().get(IWireTypeNetwork.PROPERTY_NETWORK_PORT_IS_AUTO);
					boolean autoPort = Boolean.parseBoolean(autoPortString);
					if (("TCP".equals(transport) || "SSL".equals(transport)) && (ip == null || (!autoPort && port == null))) { //$NON-NLS-1$ //$NON-NLS-2$
						valid.set(false);
					}

					// Pipe and Loop transport does not require additional attributes
				}
			}
		});

		if (valid.get()) {
			SimulatorUtils.Result result = SimulatorUtils.getSimulatorService(peerNode);
			if (result != null) {
				valid.set(result.service.isValidConfig(peerNode, result.settings));
			}
		}

		return valid.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeValidationDelegate#isVisible(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode)
	 */
	@Override
	public boolean isVisible(IPeerNode peerNode) {
	    return true;
	}
}
