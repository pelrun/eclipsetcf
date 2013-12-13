/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.navigator.filter;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerType;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.ui.navigator.nodes.PeerRedirectorGroupNode;
import org.eclipse.tcf.te.ui.views.interfaces.ICategory;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;

/**
 * Filter implementation filtering generic from the root level.
 */
public class GenericFilter extends ViewerFilter {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, final Object element) {
		boolean visible = true;

		if (element instanceof IPeerNode) {
			final AtomicReference<String> type = new AtomicReference<String>();

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					type.set(((IPeerNode)element).getPeer().getAttributes().get(IPeerNodeProperties.PROP_TYPE));
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);

			boolean belongsTo = type.get() == null || IPeerType.TYPE_GENERIC.equals(type.get());

			if (parentElement instanceof TreePath) {
				// If the direct parent is not a PeerRedirectorGroupNode, look at the very first
				// element in the tree path as parent, which is likely the category.
				Object candidate = ((TreePath)parentElement).getSegment(((TreePath)parentElement).getSegmentCount() - 1);
				if (candidate instanceof PeerRedirectorGroupNode) parentElement = candidate;
				else parentElement = ((TreePath)parentElement).getFirstSegment();
			}
			if (parentElement instanceof ICategory) {
				if (IUIConstants.ID_CAT_MY_TARGETS.equals(((ICategory)parentElement).getId()) || IUIConstants.ID_CAT_NEIGHBORHOOD.equals(((ICategory)parentElement).getId())) {
					visible = belongsTo;
				}
				else if (!IUIConstants.ID_CAT_FAVORITES.equals(((ICategory)parentElement).getId())) {
					visible = !belongsTo;
				}
			}
			else if (!(parentElement instanceof PeerRedirectorGroupNode)) {
				visible = belongsTo;
			}
		}

		return visible;
	}

}
