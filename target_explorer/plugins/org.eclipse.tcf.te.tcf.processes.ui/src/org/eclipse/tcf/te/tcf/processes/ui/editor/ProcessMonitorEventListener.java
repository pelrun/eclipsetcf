/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.editor;

import java.util.EventObject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.processes.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModelRefreshService;
import org.eclipse.tcf.te.ui.events.AbstractEventListener;
import org.eclipse.tcf.te.ui.swt.DisplayUtil;
import org.eclipse.tcf.te.ui.trees.TreeControl;
import org.eclipse.ui.views.properties.PropertySheet;

/**
 * Process monitor page event listener implementation.
 */
public class ProcessMonitorEventListener extends AbstractEventListener {
	// Reference to the parent tree control
	/* default */ final TreeControl treeControl;

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
				if (treeControl.getViewer().getControl() == null || treeControl.getViewer().getControl().isDisposed()) {
					EventManager.getInstance().removeEventListener(this);
					return;
				}
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
					if (IPeerNodeProperties.PROPERTY_CONNECT_STATE.equals(changeEvent.getEventId())) {
						// Peer node connect state changed to connected
						if (changeEvent.getNewValue().equals(Integer.valueOf(IConnectable.STATE_CONNECTED))) {
							// Get the new runtime model
							final IRuntimeModel model = ModelManager.getRuntimeModel(getPeerNode());
							// Update the tree viewer input element
							if (treeControl.getViewer().getInput() != model) {
								treeControl.getViewer().setInput(model);
							}
							// Refresh the model
							Protocol.invokeLater(new Runnable() {
								@Override
								public void run() {
									model.getService(IRuntimeModelRefreshService.class).refresh(new Callback() {
										@Override
										protected void internalDone(Object caller, IStatus status) {
											// Apply the auto expand level to the tree
											final TreeViewer treeViewer = (TreeViewer)treeControl.getViewer();
											DisplayUtil.safeAsyncExec(new Runnable() {
												@Override
												public void run() {
													treeViewer.expandToLevel(treeViewer.getAutoExpandLevel());
												}
											});
										}
									});
								}
							});
						}
						else  {
							// Trigger a refresh on the whole viewer to show the "Please connect ..." text
							treeControl.getViewer().refresh();
						}
					}
				}

				// PropertySheet reloaded
				else if (source instanceof PropertySheet) {
					treeControl.getParentPart().setFocus();
				}
			}
		}
	}

    @SuppressWarnings("cast")
    protected IPeerNode getPeerNode() {
		Object element = treeControl.getViewer().getInput();
		IPeerNode peerNode = element instanceof IPeerNode ? (IPeerNode)element : null;
		if (peerNode == null && element instanceof IAdaptable) {
			peerNode = (IPeerNode)((IAdaptable)element).getAdapter(IPeerNode.class);
		}
		return peerNode;
    }

}
