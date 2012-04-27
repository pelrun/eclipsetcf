/*******************************************************************************
 * Copyright (c) 2011 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal.adapters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.ui.navigator.LabelProviderDelegate;
import org.eclipse.tcf.te.ui.views.interfaces.categories.ICategorizable;
import org.eclipse.tcf.te.ui.views.interfaces.handler.IDeleteHandlerDelegate;
import org.eclipse.tcf.te.ui.views.interfaces.handler.IRefreshHandlerDelegate;
import org.eclipse.ui.IPersistableElement;

/**
 * Adapter factory implementation.
 */
public class AdapterFactory implements IAdapterFactory {
	// The adapter for ILabelProvider.class
	private final LabelProviderDelegate labelProvider = new LabelProviderDelegate();
	// The refresh handler delegate adapter
	private final IRefreshHandlerDelegate refreshDelegate = new RefreshHandlerDelegate();
	// The delete handler delegate adapter
	private final IDeleteHandlerDelegate deleteDelegate = new DeleteHandlerDelegate();
	// The categorizable element adapter
	private final ICategorizable categorizableAdapter = new CategorizableAdapter();

	// The adapter class.
	private Class<?>[] adapters = {
					ILabelProvider.class,
					IRefreshHandlerDelegate.class,
					IDeleteHandlerDelegate.class,
					IPersistableElement.class,
					ICategorizable.class
				};

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof IPeerModel) {
			if (ILabelProvider.class.equals(adapterType)) {
				return labelProvider;
			}
			if (IRefreshHandlerDelegate.class.equals(adapterType)) {
				return refreshDelegate;
			}
			if (IDeleteHandlerDelegate.class.equals(adapterType)) {
				return deleteDelegate;
			}
			if (IPersistableElement.class.equals(adapterType)) {
				return new PersistablePeerModel((IPeerModel)adaptableObject);
			}
			if (ICategorizable.class.equals(adapterType)) {
				return categorizableAdapter;
			}

		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
	 */
	@Override
	public Class[] getAdapterList() {
		return adapters;
	}

}
