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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;
import org.eclipse.ui.navigator.CommonViewer;


/**
 * Locator model listener implementation.
 */
public class LocatorModelListener implements ILocatorModelListener {
	private final ILocatorModel parentModel;
	/* default */ final CommonViewer viewer;

	/**
	 * Constructor.
	 *
	 * @param parent The parent locator model. Must not be <code>null</code>.
	 * @param viewer The common viewer instance. Must not be <code>null</code>.
	 */
	public LocatorModelListener(ILocatorModel parent, CommonViewer viewer) {
		Assert.isNotNull(parent);
		Assert.isNotNull(viewer);

		this.parentModel = parent;
		this.viewer = viewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener#modelChanged(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel, org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode, boolean)
	 */
	@Override
	public void modelChanged(ILocatorModel model, ILocatorNode locatorNode, boolean added) {
		if (parentModel.equals(model)) {
			// Locator model changed -> refresh the tree
			Tree tree = viewer.getTree();
			if (tree != null && !tree.isDisposed()) {
				Display display = tree.getDisplay();
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						if (viewer.getTree() != null && !viewer.getTree().isDisposed()) {
							viewer.refresh();
						}
					}
				});
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener#modelDisposed(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel)
	 */
	@Override
	public void modelDisposed(ILocatorModel model) {
	}
}
