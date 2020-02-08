/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal.adapters;

import org.eclipse.core.runtime.Assert;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.ui.views.Managers;
import org.eclipse.tcf.te.ui.views.interfaces.ICategory;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.tcf.te.ui.views.interfaces.categories.ICategorizable;

/**
 * Categorizable element adapter implementation
 */
public class CategorizableAdapter implements ICategorizable {
	// Reference to the adapted element
	/* default */ final Object element;

	/**
	 * Constructor.
	 *
	 * @param element The adapted element. Must not be <code>null</code>.
	 */
	public CategorizableAdapter(Object element) {
		Assert.isNotNull(element);
		this.element = element;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.interfaces.categories.ICategorizable#getId()
	 */
	@Override
	public String getId() {
		if (element instanceof IPeerNode) {
			return ((IPeerNode)element).getPeerId();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.interfaces.categories.ICategorizable#isValid(org.eclipse.tcf.te.ui.views.interfaces.categories.ICategorizable.OPERATION, org.eclipse.tcf.te.ui.views.interfaces.ICategory, org.eclipse.tcf.te.ui.views.interfaces.ICategory)
	 */
	@Override
	public boolean isValid(OPERATION operation, ICategory parentCategory, ICategory category) {
		Assert.isNotNull(operation);
		Assert.isNotNull(category);

		if (element instanceof IPeerNode) {
			// ADD: Parent and destination category are the same -> not valid
			if (OPERATION.ADD.equals(operation) && category.equals(parentCategory)) {
				return false;
			}

			// ALL: Static peer's cannot be removed from or added to "My Targets"
			if (IUIConstants.ID_CAT_MY_TARGETS.equals(category.getId())) {
				return true;
			}

			// ALL: Destination is "Neighborhood" -> not valid
			if (IUIConstants.ID_CAT_FAVORITES.equals(category.getId())) {
				return true;
			}
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.interfaces.categories.ICategorizable#isEnabled(org.eclipse.tcf.te.ui.views.interfaces.categories.ICategorizable.OPERATION, org.eclipse.tcf.te.ui.views.interfaces.ICategory)
	 */
	@Override
	public boolean isEnabled(OPERATION operation, ICategory category) {
		Assert.isNotNull(operation);
		Assert.isNotNull(category);

		if (element instanceof IPeerNode) {
			// ADD: element belongs to category -> not enabled
			if (OPERATION.ADD.equals(operation)
							&& Managers.getCategoryManager().belongsTo(category.getId(), getId())) {
				return false;
			}

			// REMOVE: element belongs not to category -> not enabled
			if (OPERATION.REMOVE.equals(operation)
							&& !Managers.getCategoryManager().belongsTo(category.getId(), getId())) {
				return false;
			}

			return true;
		}

		return false;
	}
}
