/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.locator.utils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.runtime.persistence.PersistenceManager;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Data helper for de/encoding .
 */
public class PeerNodeDataHelper {

	/**
	 * Encode an array of peer nodes to a string.
	 * @param contexts The array of peer nodes.
	 * @return String representing the array of peer nodes.
	 */
	public static final String encodeContextList(IPeerNode[] contexts) {
		try {
			if (contexts != null && contexts.length > 0) {
				IPersistenceDelegate delegate = PersistenceManager.getInstance().getDelegate(IPeer.class, String.class);
				return (String)delegate.writeList(contexts, String.class);
			}
		}
		catch (Exception e) {
		}
		return null;
	}

	/**
	 * Decode a string encoded array of peer nodes.
	 * @param encoded The string encoded peer nodes.
	 * @return Array of peer nodes.
	 */
	public static final IPeerNode[] decodeContextList(String encoded) {
		try {
			if (encoded != null && encoded.trim().length() > 0) {
				IPersistenceDelegate delegate = PersistenceManager.getInstance().getDelegate(IPeer.class, String.class);
				Object[] input = delegate.readList(IPeerNode.class, encoded);
				List<IPeerNode> peers = new ArrayList<IPeerNode>();
				for (Object object : input) {
					if (object instanceof IPeerNode) {
						peers.add((IPeerNode)object);
					}
				}
				return peers.toArray(new IPeerNode[peers.size()]);
			}
		}
		catch (Exception e) {
		}
		return new IPeerNode[0];
	}
}
