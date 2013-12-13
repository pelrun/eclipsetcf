/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.navigator;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.interfaces.IDisposable;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.model.Model;
import org.eclipse.tcf.te.tcf.ui.navigator.nodes.PeerRedirectorGroupNode;
import org.eclipse.ui.navigator.CommonViewer;

/**
 * Tree viewer listener implementation.
 */
public class TreeViewerListener implements ITreeViewerListener, IDisposable {
	private final CommonViewer viewer;

	/**
	 * Constructor
	 *
	 * @param viewer The common viewer instance. Must not be <code>null</code>.
	 */
	public TreeViewerListener(CommonViewer viewer) {
		Assert.isNotNull(viewer);
		this.viewer = viewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.IDisposable#dispose()
	 */
	@Override
	public void dispose() {
		viewer.removeTreeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeViewerListener#treeCollapsed(org.eclipse.jface.viewers.TreeExpansionEvent)
	 */
	@Override
	public void treeCollapsed(TreeExpansionEvent event) {
		if (event.getElement() instanceof PeerRedirectorGroupNode) {
			final List<IPeerNode> candidates = Model.getModel().getChildren(((PeerRedirectorGroupNode)event.getElement()).peerId);
			if (candidates != null && candidates.size() > 0) {
				Protocol.invokeLater(new Runnable() {
					@Override
					public void run() {
						// Mark all candidates to be excluded from the scan process
						for (final IPeerNode candidate: candidates) {
							markExcluded(candidate);
						}
					}
				});
			}
		}
	}

	/**
	 * Mark the given peer model node and it's child nodes to be excluded from the scanner.
	 *
	 * @param peerNode The peer model node. Must not be <code>null</code>.
	 */
	/* default */ void markExcluded(IPeerNode peerNode) {
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$
		Assert.isNotNull(peerNode);

		peerNode.setProperty(IPeerNodeProperties.PROP_SCANNER_EXCLUDE, true);

		List<IPeerNode> candidates = Model.getModel().getChildren(peerNode.getPeerId());
		if (candidates != null && candidates.size() > 0) {
			for (final IPeerNode candidate: candidates) {
				markExcluded(candidate);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeViewerListener#treeExpanded(org.eclipse.jface.viewers.TreeExpansionEvent)
	 */
	@Override
	public void treeExpanded(TreeExpansionEvent event) {
	}

}
