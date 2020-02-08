/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.interfaces;

import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.runtime.interfaces.extensions.IExecutableExtension;
import org.eclipse.tcf.te.ui.views.navigator.nodes.NewWizardNode;

/**
 * Main view category node.
 */
public interface ICategory extends IExecutableExtension {

	/**
	 * Returns the category image.
	 *
	 * @return The category image or <code>null</code>.
	 */
	public Image getImage();

	/**
	 * Returns the sorting rank.
	 *
	 * @return The sorting rank, or a value less than -1 to fallback to alphabetical sorting.
	 */
	public int getRank();

	/**
	 * Check whether the given categorizable element belongs to this category.
	 *
	 * @param element The categorizable element.
	 * @return <code>True</code> if the element should be shown within this category.
	 */
	public boolean belongsTo(Object element);

	/**
	 * Returns whether this category is enabled or not.
	 *
	 * @return <code>True</code> if the category is enabled, <code>false</code> otherwise.
	 */
	public boolean isEnabled();

	/**
	 * Returns an array of children that are always available in this category.
	 * This might be a list of {@link NewWizardNode}
	 * @return The children or <code>null</code>
	 */
	public Object[] getChildren();
}
