/*******************************************************************************
 * Copyright (c) 2014, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.pages;

import java.util.EventObject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.filesystem.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.ui.events.AbstractEventListener;
import org.eclipse.tcf.te.ui.trees.TreeControl;

/**
 * Filesystem page event listener implementation.
 */
public class FSExplorerEventListener extends AbstractEventListener {
	// Reference to the parent tree control
	/* default */ final TreeControl treeControl;

	/**
     * Constructor.
     *
     * @param treeControl The parent tree control. Must not be <code>null</code>.
     */
    public FSExplorerEventListener(TreeControl treeControl) {
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
				if (treeControl.getViewer().getControl() == null || treeControl.getViewer().getControl().isDisposed()) {
					EventManager.getInstance().removeEventListener(this);
					return;
				}
				// Property changes for the runtime model refreshes the whole tree.
				if (source instanceof IRuntimeModel) {
					treeControl.getViewer().refresh();
				}

				// Property changes for individual context nodes refreshes the node only
				else if (source instanceof IFSTreeNode) {
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
					if (IPeerNodeProperties.PROPERTY_CONNECT_STATE.equals(changeEvent.getEventId())) {
						// Peer node connect state changed to connected
						if (changeEvent.getNewValue().equals(Integer.valueOf(IConnectable.STATE_CONNECTED))) {
							// Get the new runtime model
							final IRuntimeModel model = ModelManager.getRuntimeModel(getPeerNode());
							if (model != null) {
								// Update the tree viewer input element
								treeControl.getViewer().setInput(model.getRoot());
							}
						}
						// Trigger a refresh on the whole viewer to show the "Please connect ..." text
						treeControl.getViewer().refresh();
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
