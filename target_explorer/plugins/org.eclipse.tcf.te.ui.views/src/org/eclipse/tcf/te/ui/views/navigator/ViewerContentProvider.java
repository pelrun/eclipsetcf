/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.navigator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.tcf.te.ui.views.activator.UIPlugin;
import org.eclipse.tcf.te.ui.views.extensions.CategoriesExtensionPointManager;
import org.eclipse.tcf.te.ui.views.interfaces.ICategory;
import org.eclipse.tcf.te.ui.views.interfaces.IRoot;
import org.eclipse.tcf.te.ui.views.internal.ViewRoot;
import org.eclipse.tcf.te.ui.views.navigator.nodes.NewWizardNode;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;

/**
 * Category content provider delegate implementation.
 */
public class ViewerContentProvider implements ICommonContentProvider {
	private final static Object[] NO_ELEMENTS = new Object[0];

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		Object[] children = NO_ELEMENTS;

		if (parentElement == null || parentElement instanceof IRoot) {
			// Get all contributed categories if there are any
			ICategory[] categories = CategoriesExtensionPointManager.getInstance().getCategories(false);
			// Filter out possible hidden categories
			List<ICategory> visibleCategories = new ArrayList<ICategory>();
			for (ICategory category : categories) {
				// If the category is not enabled by expression --> not shown
				if (!category.isEnabled()) continue;
				// Add category to the list of visible categories
				visibleCategories.add(category);
			}

			children = visibleCategories.toArray(new ICategory[visibleCategories.size()]);
		}
		if (parentElement instanceof ICategory) {
			children = ((ICategory)parentElement).getChildren();
		}

		return children;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		if (element instanceof ICategory) {
			return ViewRoot.getInstance();
		}
		if (element instanceof NewWizardNode) {
			return ((NewWizardNode)element).getParent();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof ICategory) {
			return ((ICategory)element).getChildren() != null;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.ICommonContentProvider#init(org.eclipse.ui.navigator.ICommonContentExtensionSite)
	 */
	@Override
	public void init(ICommonContentExtensionSite aConfig) {
		if (!UIPlugin.getScopedPreferences().getBoolean(getClass().getName() + ".CustomContentActivationDone")) { //$NON-NLS-1$
			String[] ids = aConfig.getService().getVisibleExtensionIds();
			for (String id : ids) {
				String prefKey = id + ".activate"; //$NON-NLS-1$
				if (UIPlugin.getScopedPreferences().containsKey(prefKey)) {
					boolean active = aConfig.getService().isActive(id);
					boolean newActive = UIPlugin.getScopedPreferences().getBoolean(prefKey) || Boolean.getBoolean(prefKey);
					if (active != newActive) {
						if (newActive) {
							aConfig.getService().getActivationService().activateExtensions(new String[]{id}, false);
						}
						else {
							aConfig.getService().getActivationService().deactivateExtensions(new String[]{id}, false);
						}
						aConfig.getService().getActivationService().persistExtensionActivations();
					}
				}
	        }
			UIPlugin.getScopedPreferences().putBoolean(getClass().getName() + ".CustomContentActivationDone", true); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IMementoAware#restoreState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void restoreState(IMemento aMemento) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.IMementoAware#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento aMemento) {
	}
}
