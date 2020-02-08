/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.core.internal;

import java.util.EventObject;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.tcf.core.events.DeletedEvent;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

/**
 * Event listener implementation.
 */
public class EventListener implements IEventListener {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventListener#eventFired(java.util.EventObject)
	 */
	@Override
	public void eventFired(EventObject event) {
		if (event instanceof DeletedEvent) {
			IPeerNode node = (IPeerNode) ((DeletedEvent) event).getPeerNode();
			ILaunchConfiguration configuration = node != null ? (ILaunchConfiguration) node.getAdapter(ILaunchConfiguration.class) : null;
			if (configuration != null) {
				try {
					configuration.delete();
				} catch (CoreException e) {
					e.printStackTrace();
					/* Ignored on purpose */
				}
			}
		}
	}

}
