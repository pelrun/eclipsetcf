/*******************************************************************************
 * Copyright (c) 2010 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - Initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.internal.target.core;

import org.eclipse.tcf.target.core.AbstractTarget;
import org.eclipse.tcf.target.core.ITarget;
import org.eclipse.tm.tcf.protocol.IPeer;

public class DiscoveredTarget extends AbstractTarget implements ITarget {

	public DiscoveredTarget(IPeer firstPeer) {
		handleNewPeer(firstPeer);
	}
	
	@Override
	protected void launch() {
		// Discovered targets have already launched
	}
	
	@Override
	public boolean handleRemovePeer(String id) {
		super.handleRemovePeer(id); // ignore result
		
		return peers.isEmpty(); // return true if peers all gone
	}
	
}
