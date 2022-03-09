/*******************************************************************************
 * Copyright (c) 2011-2022 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.internal.categories;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.ICountable;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.tcf.te.ui.views.Managers;
import org.eclipse.tcf.te.ui.views.ViewsUtil;
import org.eclipse.tcf.te.ui.views.activator.UIPlugin;
import org.eclipse.tcf.te.ui.views.extensions.CategoriesExtensionPointManager;
import org.eclipse.tcf.te.ui.views.handler.CategoryAddToContributionItem;
import org.eclipse.tcf.te.ui.views.interfaces.ICategory;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.tcf.te.ui.views.interfaces.categories.ICategorizable;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.INavigatorActivationService;
import org.eclipse.ui.navigator.INavigatorContentService;

/**
 * Category property tester.
 */
public class CategoryPropertyTester extends PropertyTester {

	/* (non-Javadoc)
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		if (receiver instanceof IStructuredSelection) {
			// Analyze the selection
			return testSelection((IStructuredSelection)receiver, property, args, expectedValue);
		}

		return internalTest(receiver, property, args, expectedValue);
	}

	/**
	 * Test the specific selection properties.
	 *
	 * @param selection The selection. Must not be <code>null</code>.
	 * @param property The property to test.
	 * @param args The property arguments.
	 * @param expectedValue The expected value.
	 *
	 * @return <code>True</code> if the property to test has the expected value, <code>false</code>
	 *         otherwise.
	 */
    protected boolean testSelection(IStructuredSelection selection, String property, Object[] args, Object expectedValue) {
		Assert.isNotNull(selection);

		if ("parentCategoryId".equals(property) && !selection.isEmpty()) { //$NON-NLS-1$
			// Only single element selection is supported
			Object element = selection.getFirstElement();

			TreePath[] pathes = selection instanceof ITreeSelection ? ((ITreeSelection)selection).getPathsFor(element) : null;
			if (pathes != null) {
				for (TreePath path : pathes) {
					// Find the category within the tree path
					TreePath parentPath = path.getParentPath();
					while (parentPath != null) {
						Object lastSegment = parentPath.getLastSegment();
						if (lastSegment instanceof ICategory
								&& ((ICategory)lastSegment).getId().equals(expectedValue)) {
							return true;
						}
						parentPath = parentPath.getParentPath();
					}
				}
			}
		}

		if ("validAddToCategoriesCount".equals(property)) { //$NON-NLS-1$
			// Determine the number of valid "Add To" Categories
			AbstractCategoryContributionItem item = new CategoryAddToContributionItem();
			ICategory[] categories = item.getCategories(selection, true);
			final int count = categories.length;

			// Re-use the count expression to allow the same value syntax
			String value = expectedValue instanceof String ? (String)expectedValue : expectedValue != null ? expectedValue.toString() : null;
			if (value != null) {
				CountExpression expression =  new CountExpression(value);
				IEvaluationContext context = new EvaluationContext(null, new ICountable() {
					@Override
					public int count() {
						return count;
					}
				});

				EvaluationResult result = EvaluationResult.FALSE;
				try {
					result = expression.evaluate(context);
				} catch (CoreException e) {
					if (Platform.inDebugMode()) e.printStackTrace();
				}

				return result.equals(EvaluationResult.TRUE);
			}
		}

		return false;
	}

	/**
	 * Internal helper to {@link #test(Object, String, Object[], Object)}.
	 */
	protected boolean internalTest(Object receiver, String property, Object[] args, Object expectedValue) {
		ICategorizable categorizable = receiver instanceof IAdaptable ? (ICategorizable)((IAdaptable)receiver).getAdapter(ICategorizable.class) : null;
		if (categorizable == null) categorizable = (ICategorizable)Platform.getAdapterManager().getAdapter(receiver, ICategorizable.class);

		if ("belongsTo".equals(property) && categorizable != null) { //$NON-NLS-1$
			String id = categorizable.getId();
			if (id != null && expectedValue instanceof String) {
				return Managers.getCategoryManager().belongsTo((String)expectedValue, id);
			}
		}

		if ("isCategoryID".equals(property) && receiver instanceof ICategory) { //$NON-NLS-1$
			String id = ((ICategory)receiver).getId();
			return id.equals(expectedValue);
		}

		if ("isCategoryEnabled".equals(property) && expectedValue instanceof Boolean) { //$NON-NLS-1$
			String categoryId = args != null && args.length == 1 && args[0] instanceof String ? (String)args[0] : null;
			ICategory category = categoryId != null ? CategoriesExtensionPointManager.getInstance().getCategory(categoryId, false) : null;

			return category != null && (category.isEnabled() == ((Boolean)expectedValue).booleanValue());
		}

		if ("isHiddenByPreferences".equals(property) && receiver instanceof ICategory && expectedValue instanceof Boolean) { //$NON-NLS-1$
			String prefKey = ((ICategory)receiver).getId() + ".hide"; //$NON-NLS-1$
			boolean isHidden = UIPlugin.getScopedPreferences().getBoolean(prefKey) || Boolean.getBoolean(prefKey);
			return ((Boolean)expectedValue).booleanValue() == isHidden;
		}

		if ("isVisibleNavigatorContent".equals(property) && receiver instanceof ICategory && expectedValue instanceof Boolean) { //$NON-NLS-1$
			boolean isVisible = false;

			String navContentID = args != null && args.length == 1 && args[0] instanceof String ? (String)args[0] : null;
			IWorkbenchPart part = ViewsUtil.getPart(IUIConstants.ID_EXPLORER);

			if (part instanceof CommonNavigator) {
				CommonNavigator navigator = (CommonNavigator)part;
				INavigatorContentService service = navigator.getNavigatorContentService();
				INavigatorActivationService activationService = service != null ? service.getActivationService() : null;
				isVisible = activationService != null && navContentID != null && activationService.isNavigatorExtensionActive(navContentID);
			}
			else {
				return true;
			}

			return ((Boolean)expectedValue).booleanValue() == isVisible;
		}

		return false;
	}
}
