/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
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
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerType;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
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
					type.set(((IPeerNode)element).getPeer().getAttributes().get(IPeerProperties.PROP_TYPE));
				}
			};

			if (Protocol.isDispatchThread()) runnable.run();
			else Protocol.invokeAndWait(runnable);

			boolean belongsTo = type.get() == null || IPeerType.TYPE_GENERIC.equals(type.get());

			if (parentElement instanceof TreePath) {
				// Look at the very first element in the tree path as parent, which is likely the category.
				parentElement = ((TreePath)parentElement).getFirstSegment();
			}
			if (parentElement instanceof ICategory) {
				if (IUIConstants.ID_CAT_MY_TARGETS.equals(((ICategory)parentElement).getId()) || IUIConstants.ID_CAT_NEIGHBORHOOD.equals(((ICategory)parentElement).getId())) {
					visible = belongsTo;
				}
				else if (!IUIConstants.ID_CAT_FAVORITES.equals(((ICategory)parentElement).getId())) {
					visible = !belongsTo;
				}
			}
			else {
				visible = belongsTo;
			}
		}

		return visible;
	}

}
