/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.trees;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Abstract tree control content contribution.
 */
public abstract class AbstractContentContribution extends PlatformObject {
	// The reference to the tree content provider instance
	private ITreeContentProvider contentProvider;

	/**
	 * Dispose the content contribution.
	 */
	public void dispose() {
		if (contentProvider != null) {
			contentProvider.dispose();
			contentProvider = null;
		}
	}

	/**
	 * Returns the tree content provider instance. If not yet created,
	 * this method will call {@link #doCreateTreeContentProvider()} to
	 * create the tree content provider instance.
	 *
	 * @return The tree content provider instance.
	 */
	public final ITreeContentProvider getContentProvider() {
		if (contentProvider == null) {
			contentProvider = doCreateTreeContentProvider();
			Assert.isNotNull(contentProvider);
		}
		return contentProvider;
	}

	/**
	 * Creates the tree content provider instance.
	 *
	 * @return The tree content provider instance. Must not return <code>null</code>.
	 */
	protected abstract ITreeContentProvider doCreateTreeContentProvider();

	/**
	 * Returns the column text for the column with the given id and the given element.
	 *
	 * @return The column text or <code>null</code>.
	 */
	protected abstract String getColumnText(String columnId, Object element);

	/**
	 * Returns the column image for the column with the given id and the given element.
	 *
	 * @return The column image or <code>null</code>.
	 */
	protected abstract Image getColumnImage(String columnId, Object element);

	/**
	 * Invoked from the tree control content and label provider to determine
	 * if a given element is handled by this content contribution.
	 *
	 * @param element The element or <code>null</code>.
	 * @return <code>True</code> if the element is handled by this content contribution, <code>false</code> otherwise.
	 */
	public abstract boolean isElementHandled(Object element);
}
