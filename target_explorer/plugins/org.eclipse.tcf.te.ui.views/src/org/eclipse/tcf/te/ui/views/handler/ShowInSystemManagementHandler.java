/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.handler;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.tcf.te.ui.views.extensions.CategoriesExtensionPointManager;
import org.eclipse.tcf.te.ui.views.interfaces.ICategory;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.part.EditorPart;

/**
 * "Show In System Management" command handler implementation.
 */
public class ShowInSystemManagementHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Get the active part
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		// The element to select
		Object element = null;

		// If the handler is invoked from an editor part, ignore the selection and
		// construct an artificial selection from the active editor input.
		if (part instanceof EditorPart) {
			IEditorInput input = ((EditorPart)part).getEditorInput();
			element = input != null ? input.getAdapter(Object.class) : null;
		}

		if (element != null) {
			setAndCheckSelection(IUIConstants.ID_EXPLORER, element);
		}

		return null;
	}

	public static void setAndCheckSelection(final String id, final Object element) {
		Assert.isNotNull(id);

		final AtomicReference<IViewPart> viewPart = new AtomicReference<IViewPart>();
		// Create the runnable
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// Check the active workbench window and active page instances
				if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null && PlatformUI
				                .getWorkbench().getActiveWorkbenchWindow().getActivePage() != null) {
					// show the view
					try {
						viewPart.set(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(id));
					}
					catch (Exception e) {
					}
				}
			}
		};
		// Execute asynchronously
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getDisplay().syncExec(runnable);
		}

		// Create the runnable
		runnable = new Runnable() {
			@Override
			public void run() {
				IViewPart part = viewPart.get();
				((CommonNavigator)part).getCommonViewer().setSelection(new StructuredSelection(element), true);
				IStructuredSelection newSel = (IStructuredSelection)((CommonNavigator)part).getCommonViewer().getSelection();
				if (newSel == null || newSel.isEmpty() || !newSel.getFirstElement().equals(element)) {
					for (ICategory category : CategoriesExtensionPointManager.getInstance().getCategories(false)) {
						if (category.belongsTo(element)) {
            				if (part instanceof CommonNavigator) {
	            				((CommonNavigator)part).getCommonViewer().setSelection(new StructuredSelection(category), true);
            					((CommonNavigator)part).getCommonViewer().expandToLevel(category, 1);
            				}
            				((CommonNavigator)part).getCommonViewer().setSelection(new StructuredSelection(element), true);
            				newSel = (IStructuredSelection)((CommonNavigator)part).getCommonViewer().getSelection();
        					if (newSel != null && !newSel.isEmpty() && newSel.getFirstElement().equals(element)) {
        						return;
        					}
                        }
					}
				}
			}
		};

		// Execute asynchronously
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
		}
	}
}
