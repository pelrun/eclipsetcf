/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.ui.interfaces.handler.IEditorHandlerDelegate;
import org.eclipse.tcf.te.ui.views.activator.UIPlugin;
import org.eclipse.tcf.te.ui.views.editor.EditorInput;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.tcf.te.ui.views.nls.Messages;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.navigator.CommonNavigator;

/**
 * TCF tree elements open command handler implementation.
 */
public class OpenEditorHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
    @Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// The selection is the Target Explorer tree selection
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		// The active part is the Target Explorer view instance
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		// Get the currently active workbench window
        // In Eclipse 4.x, the HandlerUtil.getActiveWorkbenchWindow(event) may return null
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        if (window == null) window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		// ALT - Key pressed?
		Object ctrlPressed = HandlerUtil.getVariable(event, "ctrlPressed"); //$NON-NLS-1$

		boolean expand = ctrlPressed instanceof Boolean ? ((Boolean)ctrlPressed).booleanValue() : false;

		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			// If the tree node is expandable, expand or collapse it
			TreeViewer viewer = ((CommonNavigator)part).getCommonViewer();
			Object element = ((IStructuredSelection)selection).getFirstElement();
			if (selection instanceof TreeSelection) {
				TreePath[] path = ((TreeSelection)selection).getPaths();
				if (path != null && path.length > 0) {
					element = path[0].getLastSegment();
				}
			}
			if (viewer.isExpandable(element) && expand) {
				viewer.setExpandedState(element, !viewer.getExpandedState(element));
			}
			else {
				openEditorOnSelection(window, selection);
			}
		}

		return null;
	}

	/**
	 * Opens the properties editor in the given workbench window on the given selection.
	 *
	 * @param window The workbench window. Must not be <code>null</code>.
	 * @param selection The selection. Must not be <code>null</code>.
	 */
	public static void openEditorOnSelection(IWorkbenchWindow window, ISelection selection) {
		Assert.isNotNull(window);
		Assert.isNotNull(selection);

		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object element = ((IStructuredSelection)selection).getFirstElement();
			if (element != null) {
				// Get the active page
				IWorkbenchPage page = window.getActivePage();
				// Create the editor input object
				IEditorHandlerDelegate delegate = ServiceUtils.getUIServiceDelegate(element, element, IEditorHandlerDelegate.class);
				IEditorInput input = delegate != null ? delegate.getEditorInput(element) : new EditorInput(element);
				try {
					// Opens the Target Explorer properties editor
					IEditorPart editor = page.openEditor(input, IUIConstants.ID_EDITOR);
					// Lookup the ui service for post action
					if (delegate != null)
						delegate.postOpenEditor(editor, element);
				} catch (PartInitException e) {
					IStatus status = new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(), Messages.OpenCommandHandler_error_openEditor, e);
					UIPlugin.getDefault().getLog().log(status);
				}
			}
		}
	}
}
