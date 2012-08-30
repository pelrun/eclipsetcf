/**
 * EventListener.java
 * Created on Jan 28, 2012
 *
 * Copyright (c) 2012 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.processes.ui.navigator.events;

import java.util.EventObject;

import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.ui.views.events.AbstractEventListener;

/**
 * UI event listener updating the main view.
 */
public class EventListener extends AbstractEventListener {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventListener#eventFired(java.util.EventObject)
	 */
	@Override
    public void eventFired(EventObject event) {
		if (event instanceof ChangeEvent) {
			final ChangeEvent changeEvent = (ChangeEvent)event;
			final Object source = changeEvent.getSource();

			// Property changes for the runtime model refreshes the parent peer
			// node. The runtime model is not visible by itself.
			if (source instanceof IRuntimeModel) {
				IPeerModel node = (IPeerModel)((IRuntimeModel)source).getAdapter(IPeerModel.class);
				refresh(node, false);
			}

			// Property changes for individual context nodes refreshes the node only
			else if (source instanceof IProcessContextNode) {
				if ("expanded".equals(changeEvent.getEventId())) { //$NON-NLS-1$
					// Expansion state of the node changed.
					boolean expanded = ((Boolean)changeEvent.getNewValue()).booleanValue();
					// Update the nodes expansion state
					getViewer().setExpandedState(source, expanded);
				} else {
					refresh(source, false);
				}
			}
		}
	}

}
