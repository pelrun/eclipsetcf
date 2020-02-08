/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.navigator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.listener.ModelAdapter;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.ActivityManagerEvent;
import org.eclipse.ui.activities.IActivityManagerListener;
import org.eclipse.ui.navigator.CommonViewer;


/**
 * Peer model listener implementation.
 */
public class PeerModelListener extends ModelAdapter implements IActivityManagerListener {
	private final IPeerModel parentModel;
	/* default */ final CommonViewer viewer;

	/**
	 * Constructor.
	 *
	 * @param parent The parent peer model. Must not be <code>null</code>.
	 * @param viewer The common viewer instance. Must not be <code>null</code>.
	 */
	public PeerModelListener(IPeerModel parent, CommonViewer viewer) {
		Assert.isNotNull(parent);
		Assert.isNotNull(viewer);

		this.parentModel = parent;
		this.viewer = viewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.listener.ModelAdapter#modelChanged(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel, org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, boolean)
	 */
	@Override
	public void modelChanged(final IPeerModel model, final IPeerNode peerNode, final boolean added) {
		if (parentModel.equals(model)) {
			// Locator model changed -> refresh the tree
			Tree tree = viewer.getTree();
			if (tree != null && !tree.isDisposed()) {
				Display display = tree.getDisplay();
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						if (viewer.getTree() != null && !viewer.getTree().isDisposed()) {
							viewer.refresh(true);
						}
					}
				});
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.activities.IActivityManagerListener#activityManagerChanged(org.eclipse.ui.activities.ActivityManagerEvent)
	 */
    @Override
    public void activityManagerChanged(ActivityManagerEvent activityManagerEvent) {
    	if (activityManagerEvent.haveEnabledActivityIdsChanged()) {
			Tree tree = viewer.getTree();
			if (tree != null && !tree.isDisposed()) {
				Display display = tree.getDisplay();
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						if (viewer.getTree() != null && !viewer.getTree().isDisposed()) {
							viewer.refresh(true);
						}
					}
				});
			}

			for (IEditorReference ref : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences()) {
				try {
					IPeerNode peerNode = getPeerNode(ref.getEditorInput());
					if (peerNode != null && !peerNode.isVisible()) {
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditors(new IEditorReference[]{ref}, true);
					}
				}
				catch (Exception e) {
				}
            }

    	}
    }

	protected IPeerNode getPeerNode(Object element) {
		IPeerNode peerNode = null;
		if (element instanceof IPeerNode) {
			peerNode = (IPeerNode)element;
		}
		else if (element instanceof IAdaptable) {
			peerNode = (IPeerNode)((IAdaptable)element).getAdapter(IPeerNode.class);
		}
		if (peerNode == null) {
			peerNode = (IPeerNode)Platform.getAdapterManager().getAdapter(element, IPeerNode.class);
		}

		return peerNode;
	}
}
