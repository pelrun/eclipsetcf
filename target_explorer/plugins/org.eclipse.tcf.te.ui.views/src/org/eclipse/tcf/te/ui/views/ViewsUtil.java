/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.tcf.te.ui.views.activator.UIPlugin;
import org.eclipse.tcf.te.ui.views.extensions.CategoriesExtensionPointManager;
import org.eclipse.tcf.te.ui.views.handler.OpenEditorHandler;
import org.eclipse.tcf.te.ui.views.interfaces.ICategory;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.tcf.te.ui.views.nls.Messages;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.navigator.CommonNavigator;

/**
 * Utility methods to deal with views.
 */
public class ViewsUtil {

	/**
	 * Returns the workbench part identified by the given id.
	 *
	 * @param id The view id. Must not be <code>null</code>.
	 * @return The workbench part or <code>null</code>.
	 */
	public static IWorkbenchPart getPart(final String id) {
		Assert.isNotNull(id);

		final AtomicReference<IWorkbenchPart> part = new AtomicReference<IWorkbenchPart>(null);
		// Create the runnable
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// Check the active workbench window and active page instances
				if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null) {
					// Get the view reference
					IViewReference reference = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findViewReference(id);
					// Return the view part from the reference, but do not restore it
					part.set(reference != null ? reference.getPart(false) : null);
				}
			}
		};

		// Execute asynchronously
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getDisplay().syncExec(runnable);
		}
		return part.get();
	}

	/**
	 * Asynchronously show the view identified by the given id.
	 *
	 * @param id The view id. Must not be <code>null</code>.
	 */
	public static void show(final String id) {
		Assert.isNotNull(id);
		// Create the runnable
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// Check the active workbench window and active page instances
				if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null) {
					// Show the view
					try {
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(id);
					}
					catch (PartInitException e) { /* ignored on purpose */
					}
				}
			}
		};

		// Execute asynchronously
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
		}
	}

	/**
	 * Asynchronously refresh the view identified by the given id.
	 *
	 * @param id The view id. Must not be <code>null</code>.
	 */
	public static void refresh(final String id) {
		Assert.isNotNull(id);

		// Create the runnable
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// Check the active workbench window and active page instances
				if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null) {
					// Get the view reference
					IViewReference reference = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findViewReference(id);
					// Get the view part from the reference, but do not restore it
					IWorkbenchPart part = reference != null ? reference.getPart(false) : null;
					// If the part is a common navigator, get the common viewer
					Viewer viewer = part instanceof CommonNavigator ? ((CommonNavigator) part).getCommonViewer() : null;
					// If not a common navigator, try to adapt to the viewer
					if (viewer == null) viewer = part != null ? (Viewer) part.getAdapter(Viewer.class) : null;
					// Refresh the viewer
					if (viewer != null) viewer.refresh();
				}
			}
		};

		// Execute asynchronously
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
		}
	}

	/**
	 * Asynchronously refresh the given element within the view identified by the given id.
	 *
	 * @param id The view id. Must not be <code>null</code>.
	 * @param element The element to refresh. Must not be <code>null</code>.
	 */
	public static void refresh(final String id, final Object element) {
		Assert.isNotNull(id);
		Assert.isNotNull(element);

		// Create the runnable
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// Check the active workbench window and active page instances
				if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null) {
					// Get the view reference
					IViewReference reference = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findViewReference(id);
					// Get the view part from the reference, but do not restore it
					IWorkbenchPart part = reference != null ? reference.getPart(false) : null;
					// If the part is a common navigator, get the common viewer
					Viewer viewer = part instanceof CommonNavigator ? ((CommonNavigator) part).getCommonViewer() : null;
					// If not a common navigator, try to adapt to the viewer
					if (viewer == null) viewer = part != null ? (Viewer) part.getAdapter(Viewer.class) : null;
					// Refresh the viewer
					if (viewer instanceof StructuredViewer) ((StructuredViewer) viewer).refresh(element, true);
					else if (viewer != null) viewer.refresh();
				}
			}
		};

		// Execute asynchronously
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
		}
	}

	/**
	 * Asynchronously set the given selection to the view identified by the given id.
	 *
	 * @param id The view id. Must not be <code>null</code>.
	 * @param selection The selection or <code>null</code>.
	 */
	public static void setSelection(final String id, final ISelection selection) {
		Assert.isNotNull(id);

		// Create the runnable
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// Check the active workbench window and active page instances
				if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null) {
					// Get the view reference
					IViewReference reference = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findViewReference(id);
					// Get the view part from the reference, but do not restore it
					IWorkbenchPart part = reference != null ? reference.getPart(false) : null;
					// Get the selection provider
					ISelectionProvider selectionProvider = part != null && part.getSite() != null ? part.getSite().getSelectionProvider() : null;
					// And apply the selection
					if (selectionProvider != null) selectionProvider.setSelection(selection);
				}
			}
		};

		// Execute asynchronously
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
		}
	}

	/**
	 * Opens the editor on the given selection.
	 *
	 * @param selection The selection. Must not be <code>null</code>.
	 */
	public static void openEditor(final ISelection selection) {
		Assert.isNotNull(selection);

		// Create the runnable
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				Assert.isNotNull(window);
				OpenEditorHandler.openEditorOnSelection(window, selection);
			}
		};

		// Execute asynchronously
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
		}
	}

	/**
	 * Reopens the given editor and set the given page as active page.
	 *
	 * @param editor The editor. Must not be <code>null</code>.
	 * @param pageId The id of the active page or <code>null</code>.
	 * @param save <code>True</code> to save the editor contents if required, <code>false</code> to discard any unsaved changes.

	 */
	public static void reopenEditor(final IEditorPart editor, final String pageId, final boolean save) {
		Assert.isNotNull(editor);

		// Create the runnable
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				Assert.isNotNull(window);
				IWorkbenchPage page = window.getActivePage();
				Assert.isNotNull(page);

				// Determine the editor input
				IEditorInput input = editor.getEditorInput();
				// Close the editor
				page.closeEditor(editor, save);
				try {
					// Reopen the editor
	                IEditorPart newEditor = page.openEditor(input, IUIConstants.ID_EDITOR);
	                // Set the active page
	                if (newEditor instanceof FormEditor && pageId != null) {
	                	((FormEditor)newEditor).setActivePage(pageId);
	                }
                }
                catch (PartInitException e) {
					IStatus status = new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(), Messages.ViewsUtil_reopen_error, e);
					UIPlugin.getDefault().getLog().log(status);
                }


			}
		};

		// Execute asynchronously
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
		}
	}

	/**
	 * "Go Into" the category identified by the given category id, within the view identified by the
	 * given id.
	 * <p>
	 * <b>Note:</b> This method is actively changing the selection of the view.
	 *
	 * @param id The view id. Must not be <code>null</code>.
	 * @param categoryId The category id. Must not be <code>null</code>.
	 */
	public static void goInto(final String id, final String categoryId) {
		Assert.isNotNull(id);
		Assert.isNotNull(categoryId);

		ICategory category = CategoriesExtensionPointManager.getInstance().getCategory(categoryId, false);
		if (category != null) goInto(id, category);
	}

	/**
	 * "Go Into" the given node within the view identified by the given id.
	 * <p>
	 * <b>Note:</b> This method is actively changing the selection of the view.
	 *
	 * @param id The view id. Must not be <code>null</code>.
	 * @param node The node to go into. Must not be <code>null</code>.
	 */
	public static void goInto(final String id, final Object node) {
		Assert.isNotNull(id);
		Assert.isNotNull(node);

		goInto(id, new StructuredSelection(node));
	}

	/**
	 * "Go Into" the given selection within the view identified by the given id.
	 * <p>
	 * <b>Note:</b> This method is actively changing the selection of the view.
	 *
	 * @param id The view id. Must not be <code>null</code>.
	 * @param selection The selection. Must not be <code>null</code>.
	 */
	public static void goInto(final String id, final ISelection selection) {
		Assert.isNotNull(id);
		Assert.isNotNull(selection);

		// Set the selection
		setSelection(id, selection);

		// Create the runnable
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// Check the active workbench window and active page instances
				if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() != null) {
					// Get the view reference
					IViewReference reference = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findViewReference(id);
					// Get the view part from the reference, but do not restore it
					IWorkbenchPart part = reference != null ? reference.getPart(false) : null;
					// Get the action bars
					IActionBars actionBars = part != null && part.getSite() instanceof IViewSite ? ((IViewSite) part.getSite()).getActionBars() : null;
					// Get the "Go Into" action
					IAction action = actionBars != null ? actionBars.getGlobalActionHandler(IWorkbenchActionConstants.GO_INTO) : null;
					// Run the action
					if (action != null) action.run();
				}
			}
		};

		// Execute asynchronously
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
		}
	}
}
