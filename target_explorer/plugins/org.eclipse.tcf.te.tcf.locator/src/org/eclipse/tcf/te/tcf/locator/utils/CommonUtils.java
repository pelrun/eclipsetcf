/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Common utils
 */
public final class CommonUtils {

	public static String getType(IPeerNode peerNode) {
		if (peerNode != null) {
			return peerNode.getPeerType();
		}
		return null;
	}

	public static String getType(IPeer peer) {
		if (peer != null) {
			return peer.getAttributes().get(IPeerProperties.PROP_TYPE);
		}
		return null;
	}

	public static String getType(IPropertiesContainer props) {
		return props != null ? props.getStringProperty(IPeerProperties.PROP_TYPE) : null;
	}

	public static String getSubType(IPeerNode peerNode) {
		IPeer peer = peerNode.getPeer();
		return getSubType(peer);
	}

	public static String getSubType(IPeer peer) {
		Map<String, String> attrs = peer.getAttributes();
		String subType = attrs.get(IPeerProperties.PROP_SUBTYPE);
		if (subType == null) {
			subType = IPeerProperties.SUBTYPE_REAL;
		}
		return subType;
	}

	public static String getSubType(IPropertiesContainer props) {
		String subType = props.getStringProperty(IPeerProperties.PROP_SUBTYPE);
		if (subType == null) {
			subType = IPeerProperties.SUBTYPE_REAL;
		}
		return subType;
	}

	public static String getMode(IPeerNode peerNode) {
		IPeer peer = peerNode.getPeer();
		return getMode(peer);
	}

	public static String getMode(IPeer peer) {
		Map<String, String> attrs = peer.getAttributes();
		String mode = attrs.get(IPeerProperties.PROP_MODE);
		if (mode == null) {
			mode = IPeerProperties.MODE_RUN;
		}
		return mode;
	}

	public static String getMode(IPropertiesContainer props) {
		String mode = props.getStringProperty(IPeerProperties.PROP_MODE);
		if (mode == null) {
			mode = IPeerProperties.MODE_RUN;
		}
		return mode;
	}

	/**
	 * Get a free local port.
	 * @return The port or -1 on any errors.
	 */
	public static int getFreePort() {
		int port = -1;
		try {
			ServerSocket socket = new ServerSocket(0);
			port = socket.getLocalPort();
			socket.close();
		} catch (IOException e) { /* ignored on purpose */ }

		return port;
	}
}
