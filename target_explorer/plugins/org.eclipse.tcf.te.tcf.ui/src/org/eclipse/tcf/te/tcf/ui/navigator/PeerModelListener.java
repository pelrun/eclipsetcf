/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.navigator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IAdapterService;
import org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.listener.ModelAdapter;
import org.eclipse.tcf.te.ui.views.editor.EditorInput;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonViewer;


/**
 * Peer model listener implementation.
 */
public class PeerModelListener extends ModelAdapter {
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
							viewer.refresh();
						}
					}
				});
			}

			if (peerNode != null) {
				// Check if the peer model node can be adapted to IPeerModelListener.
				IAdapterService service = ServiceManager.getInstance().getService(peerNode, IAdapterService.class);
				IPeerModelListener listener = service != null ? service.getAdapter(peerNode, IPeerModelListener.class) : null;
				// If yes -> Invoke the adapted model listener instance
				if (listener != null) {
					listener.modelChanged(model, peerNode, added);
				}
				// If no -> Default behavior for dynamic discovered peers is to close the editor (if any).
				// 			For static peers, leave the editor untouched.
				else if (!added) {
					Display display = PlatformUI.getWorkbench().getDisplay();
					if (display != null && !display.isDisposed()) {
						display.asyncExec(new Runnable() {
							@Override
							public void run() {
								// Get the currently active workbench window
								IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
								if (window != null) {
									// Get the active page
									IWorkbenchPage page = window.getActivePage();
									// Create the editor input object
									IEditorInput input = new EditorInput(peerNode);
									// Lookup the editors matching the editor input
									IEditorReference[] editors = page.findEditors(input, IUIConstants.ID_EDITOR, IWorkbenchPage.MATCH_INPUT);
									if (editors != null && editors.length > 0) {
										// Close the editors
										page.closeEditors(editors, true);
									}
								}
							}
						});
					}
				}
			}
		}
	}
}
