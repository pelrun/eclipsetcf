/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal.tabbed;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;

/**
 * Peer general section filter implementation.
 */
public class PeerSectionFilter implements IFilter {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IFilter#select(java.lang.Object)
	 */
	@Override
	public boolean select(Object element) {
		return element instanceof IPeer || element instanceof ILocatorNode;
	}

}
