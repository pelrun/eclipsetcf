/**
 * PeerDataHelper.java
 * Created on Aug 21, 2013
 *
 * Copyright (c) 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
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
public class PeerDataHelper {

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
