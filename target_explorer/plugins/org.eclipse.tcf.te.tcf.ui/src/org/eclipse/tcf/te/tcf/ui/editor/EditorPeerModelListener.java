/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.editor;

import org.eclipse.swt.widgets.Display;
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

/**
 * Peer model listener implementation.
 */
public class EditorPeerModelListener extends ModelAdapter {

	/**
	 * Constructor.
	 */
	public EditorPeerModelListener() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.listener.ModelAdapter#modelChanged(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel, org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, boolean)
	 */
	@Override
	public void modelChanged(final IPeerModel model, final IPeerNode peerNode, final boolean added) {
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
