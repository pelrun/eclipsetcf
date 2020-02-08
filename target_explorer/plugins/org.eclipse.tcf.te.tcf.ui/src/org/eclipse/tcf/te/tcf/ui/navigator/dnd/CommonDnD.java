/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.navigator.dnd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistableNodeProperties;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.core.peers.Peer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.ui.views.Managers;
import org.eclipse.tcf.te.ui.views.interfaces.ICategory;
import org.eclipse.tcf.te.ui.views.interfaces.IRoot;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.tcf.te.ui.views.interfaces.categories.ICategorizable;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonNavigator;

/**
 * Common DND operation implementations.
 */
public class CommonDnD {

	/**
	 * If the current selection is draggable.
	 *
	 * @param selection The currently selected nodes.
	 * @return true if it is draggable.
	 */
	public static boolean isDraggable(IStructuredSelection selection) {
		if (selection.isEmpty()) {
			return false;
		}
		Object[] objects = selection.toArray();
		for (Object object : objects) {
			if (!isDraggableObject(object)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * If the specified object is a draggable element.
	 *
	 * @param object The object to be dragged.
	 * @return true if it is draggable.
	 */
	private static boolean isDraggableObject(Object object) {
		return object instanceof IPeerNode;
	}

	/**
	 * Perform the drop operation over dragged selection.
	 *
	 * @param dropAdapter The common drop adapter.
	 * @param target The target Object to be moved to.
	 * @param operations The current DnD operation.
	 * @param selection The local selection being dropped.
	 * @return true if the dropping is successful.
	 */
	public static boolean dropLocalSelection(CommonDropAdapter dropAdapter, Object target, int operations, IStructuredSelection selection) {

		boolean result = false;
		boolean refreshModel = false;

		ICategory catToSelect = null;
		Object elementToSelect = null;

		Iterator<?> iterator = selection.iterator();
		while (iterator.hasNext()) {
			Object element = iterator.next();
			if (!isDraggableObject(element)) {
				continue;
			}

			ICategorizable categorizable = getCategorizable(element);
			if (categorizable == null || !isDraggableObject(element)) {
				continue;
			}
			ICategory[] parentCategories = getParentCategories(element, selection);
			if (parentCategories.length == 0) {
				continue;
			}

			for (ICategory parentCategory : parentCategories) {
				if (target instanceof ICategory) {
					ICategory category = (ICategory) target;

					if (element instanceof IPeerNode && category.getId().equals(parentCategory.getId())) {
						List<String> usedNames = getUsedNames();
						Map<String,String> attrs = new HashMap<String,String>(((IPeerNode)element).getPeer().getAttributes());
						attrs.put(IPeer.ATTR_ID, UUID.randomUUID().toString());
						attrs.remove(IPersistableNodeProperties.PROPERTY_URI);
						int i = 0;
						String baseName = attrs.get(IPeer.ATTR_NAME);
						baseName = baseName.replaceAll("\\s*\\([\\d*]\\)$", ""); //$NON-NLS-1$ //$NON-NLS-2$
						String name = baseName + " (" + i + ")"; //$NON-NLS-1$ //$NON-NLS-2$
						while (usedNames.contains(name.toUpperCase())) {
							i++;
							name = baseName + " (" + i + ")"; //$NON-NLS-1$ //$NON-NLS-2$
						}
						attrs.put(IPeer.ATTR_NAME, name);
						IPeer newPeer = new Peer(attrs);
						// Save the new peer
						IURIPersistenceService persistenceService = ServiceManager.getInstance().getService(IURIPersistenceService.class);
						if (persistenceService != null) {
							try {
								persistenceService.write(newPeer, null);
								refreshModel = true;
								if (catToSelect == null || elementToSelect == null) {
									catToSelect = category;
									elementToSelect = newPeer;
								}
								result = true;
							}
							catch (Exception e) {
							}
						}
					}
					else if (!Managers.getCategoryManager().isLinked(category.getId(), categorizable.getId()) &&
								categorizable.isValid(ICategorizable.OPERATION.ADD, parentCategory, category)) {
						Managers.getCategoryManager().add(category.getId(), categorizable.getId());
						if (catToSelect == null || elementToSelect == null) {
							catToSelect = category;
							elementToSelect = element;
						}
						result = true;
					}
				}
				else if (target instanceof IRoot) {
					if (Managers.getCategoryManager().isLinked(parentCategory.getId(), categorizable.getId())) {
						Managers.getCategoryManager().remove(parentCategory.getId(), categorizable.getId());
						catToSelect = parentCategory;
						result = true;
					}
				}
			}
		}

		if (result) {
			final CommonNavigator cNav = getCommonNavigator();
			if (refreshModel) {
				final ICategory finalCat = catToSelect;
				final Object finalElement = elementToSelect;
				final IPeer finalNewPeer = (elementToSelect instanceof IPeer) ? (IPeer)elementToSelect : null;
                // Trigger a refresh of the model to read in the newly created static peer
                final IPeerModelRefreshService service = ModelManager.getPeerModel().getService(IPeerModelRefreshService.class);
                if (service != null) {
                	Runnable runnable = new Runnable() {
                		@Override
                        public void run() {
                            service.refresh(new Callback() {
                            	@Override
                                protected void internalDone(Object caller, org.eclipse.core.runtime.IStatus status) {
            						IPeerNode peerNode = null;
                    				if (finalNewPeer != null) {
                    					IPeerModelLookupService service = ModelManager.getPeerModel().getService(IPeerModelLookupService.class);
                    					if (service != null) {
                    						peerNode = service.lkupPeerModelById(finalNewPeer.getID());
                    					}
                    				}
                    				refresh(cNav, finalCat, peerNode != null ? peerNode : finalElement);
                            	}
                            });

                		}
                	};
                	if (Protocol.isDispatchThread())
                		runnable.run();
                	else
                		Protocol.invokeLater(runnable);
                }
                else {
    				refresh(cNav, catToSelect, elementToSelect);
                }
			}
			else {
				refresh(cNav, catToSelect, elementToSelect);
			}
		}

		return result;
	}

	protected static void refresh(final CommonNavigator cNav, final ICategory category, final Object element) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				cNav.getCommonViewer().refresh();
				if (category != null) {
					cNav.getCommonViewer().setSelection(new StructuredSelection(category), true);
					cNav.getCommonViewer().expandToLevel(category, 1);
				}
				if (element != null)
					cNav.getCommonViewer().setSelection(new TreeSelection(new TreePath(new Object[]{category, element})), true);
			}
		};

		// Execute asynchronously
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
		}
	}

	protected static CommonNavigator getCommonNavigator() {
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
						viewPart.set(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(IUIConstants.ID_EXPLORER));
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

		return viewPart.get() instanceof CommonNavigator ? (CommonNavigator)viewPart.get() : null;
	}

	/**
	 * Validate dropping when the elements being dragged are local selection.
	 *
	 * @param dropAdapter The common drop adapter.
	 * @param target The target object.
	 * @param operation The DnD operation.
	 * @param transferType The transfered data type.
	 *
	 * @return true if it is valid for dropping.
	 */
	public static boolean validateLocalSelectionDrop(CommonDropAdapter dropAdapter, Object target, int operation, TransferData transferType) {
		int overrideOperation = -1;
		boolean valid = false;

		LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
		IStructuredSelection selection = (IStructuredSelection) transfer.getSelection();

		boolean allow = true;
		boolean link = false;
		boolean copy = false;

		Iterator<?> iterator = selection.iterator();
		while (allow && iterator.hasNext()) {
			Object element = iterator.next();
			if (!isDraggableObject(element)) {
				allow = false;
				break;
			}

			ICategorizable categorizable = getCategorizable(element);
			if (categorizable == null || !isDraggableObject(element)) {
				allow = false;
				break;
			}
			ICategory[] parentCategories = getParentCategories(element, selection);
			if (parentCategories.length == 0) {
				allow = false;
			}

			for (ICategory parentCategory : parentCategories) {
				if (target instanceof ICategory) {
					ICategory category = (ICategory) target;

					if (!link && element instanceof IPeerNode && category.getId().equals(parentCategory.getId())) {
						overrideOperation = DND.DROP_COPY;
						copy = true;
					}
					else if (!copy && !Managers.getCategoryManager().isLinked(category.getId(), categorizable.getId()) &&
								categorizable.isValid(ICategorizable.OPERATION.ADD, parentCategory, category)) {
						overrideOperation = DND.DROP_LINK;
						link = true;
					}
					else {
						allow = false;
						break;
					}

				}
				else if (target instanceof IRoot) {
					overrideOperation = DND.DROP_DEFAULT;
					if (!Managers.getCategoryManager().isLinked(parentCategory.getId(), categorizable.getId())) {
						allow = false;
						break;
					}
				}
			}
		}
		valid = allow;

		if (dropAdapter != null) {
			if (!valid) {
				dropAdapter.overrideOperation(DND.DROP_NONE);
			}
			else if (overrideOperation != -1) {
				dropAdapter.overrideOperation(overrideOperation);
			}
			else {
				dropAdapter.overrideOperation(operation);
			}
		}

		return valid;
	}

	protected static ICategory[] getParentCategories(Object element, ISelection selection) {
		List<ICategory> candidates = new ArrayList<ICategory>();
		// To determine the parent category, we have to look at the tree path
		TreePath[] pathes = selection instanceof TreeSelection ? ((TreeSelection)selection).getPathsFor(element) : null;
		if (pathes != null && pathes.length > 0) {
			for (TreePath path : pathes) {
				TreePath parentPath = path.getParentPath();
				while (parentPath != null) {
					if (parentPath.getLastSegment() instanceof ICategory) {
						if (!candidates.contains(parentPath.getLastSegment())) {
							candidates.add((ICategory)parentPath.getLastSegment());
							break;
						}
					}
					parentPath = parentPath.getParentPath();
				}
			}
		}
		return candidates.toArray(new ICategory[candidates.size()]);
	}

	protected static ICategorizable getCategorizable(Object element) {
	    ICategorizable categorizable = element instanceof IAdaptable ? (ICategorizable)((IAdaptable)element).getAdapter(ICategorizable.class) : null;
    	if (categorizable == null) categorizable = (ICategorizable)Platform.getAdapterManager().getAdapter(element, ICategorizable.class);
    	return categorizable;
	}

	protected static List<String> getUsedNames() {
		final List<String> usedNames = new ArrayList<String>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// Get all peer model objects
				IPeerNode[] peers = ModelManager.getPeerModel().getPeerNodes();
				// Loop them and find the ones which are of our handled types
				for (IPeerNode peerNode : peers) {
						String name = peerNode.getPeer().getName();
						Assert.isNotNull(name);
						if (!"".equals(name) && !usedNames.contains(name)) { //$NON-NLS-1$
							usedNames.add(name.trim().toUpperCase());
						}
				}
			}
		};

		if (Protocol.isDispatchThread())
			runnable.run();
		else
			Protocol.invokeAndWait(runnable);
		return usedNames;
	}
}
