/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.editor;

import java.util.EventObject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.processes.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModelRefreshService;
import org.eclipse.tcf.te.ui.events.AbstractEventListener;
import org.eclipse.tcf.te.ui.trees.TreeControl;

/**
 * Process monitor page event listener implementation.
 */
public class ProcessMonitorEventListener extends AbstractEventListener {
	// Reference to the parent tree control
	private final TreeControl treeControl;

	/**
     * Constructor.
     *
     * @param treeControl The parent tree control. Must not be <code>null</code>.
     */
    public ProcessMonitorEventListener(TreeControl treeControl) {
    	Assert.isNotNull(treeControl);
    	this.treeControl = treeControl;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventListener#eventFired(java.util.EventObject)
	 */
	@Override
	public void eventFired(EventObject event) {
		if (event instanceof ChangeEvent) {
			final ChangeEvent changeEvent = (ChangeEvent)event;
			final Object source = changeEvent.getSource();

			if (treeControl.getViewer() != null) {
				// Property changes for the runtime model refreshes the whole tree.
				if (source instanceof IRuntimeModel) {
					treeControl.getViewer().refresh();
				}

				// Property changes for individual context nodes refreshes the node only
				else if (source instanceof IProcessContextNode) {
					if ("expanded".equals(changeEvent.getEventId())) { //$NON-NLS-1$
						// Expansion state of the node changed.
						boolean expanded = ((Boolean)changeEvent.getNewValue()).booleanValue();
						// Update the nodes expansion state
						((TreeViewer)treeControl.getViewer()).setExpandedState(source, expanded);
					} else {
						((TreeViewer)treeControl.getViewer()).refresh(source, true);
					}
				}

				else if (source instanceof IPeerNode && source == getPeerNode()) {
					if (IPeerNodeProperties.PROP_CONNECT_STATE.equals(changeEvent.getEventId()) &&
									changeEvent.getNewValue().equals(new Integer(IConnectable.STATE_CONNECTED))) {
						Protocol.invokeLater(new Runnable() {
							@Override
							public void run() {
								ModelManager.getRuntimeModel(getPeerNode()).getService(IRuntimeModelRefreshService.class).refresh(null);
							}
						});
					}
				}
			}
		}
	}

    protected IPeerNode getPeerNode() {
		Object element = treeControl.getViewer().getInput();
		IPeerNode peerNode = element instanceof IPeerNode ? (IPeerNode)element : null;
		if (peerNode == null && element instanceof IAdaptable) {
			peerNode = (IPeerNode)((IAdaptable)element).getAdapter(IPeerNode.class);
		}
		return peerNode;
    }

}
