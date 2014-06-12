/**
 * PeerDataHelper.java
 * Created on Jun 12, 2014
 *
 * Copyright (c) 2014 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.core.util.persistence;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.runtime.persistence.PersistenceManager;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistenceDelegate;

/**
 * Data helper for encoding or decoding a peer list.
 */
public class PeerDataHelper {

	/**
	 * Encode an array of peers to a string.
	 * @param contexts The array of peers.
	 * @return String representing the array of peers.
	 */
	public static final String encodePeerList(IPeer[] contexts) {
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
	 * Decode a string encoded array of peers.
	 * @param encoded The string encoded peers.
	 * @return Array of peers.
	 */
	public static final IPeer[] decodePeerList(String encoded) {
		try {
			if (encoded != null && encoded.trim().length() > 0) {
				IPersistenceDelegate delegate = PersistenceManager.getInstance().getDelegate(IPeer.class, String.class);
				Object[] input = delegate.readList(IPeer.class, encoded);
				List<IPeer> peers = new ArrayList<IPeer>();
				for (Object object : input) {
					if (object instanceof IPeer) {
						peers.add((IPeer)object);
					}
				}
				return peers.toArray(new IPeer[peers.size()]);
			}
		}
		catch (Exception e) {
		}
		return new IPeer[0];
	}
}
