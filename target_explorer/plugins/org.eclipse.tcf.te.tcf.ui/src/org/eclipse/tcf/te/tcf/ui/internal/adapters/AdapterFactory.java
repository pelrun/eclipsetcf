/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal.adapters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.ui.views.editor.EditorInput;
import org.eclipse.tcf.te.ui.views.interfaces.IEditorSaveAsAdapter;
import org.eclipse.tcf.te.ui.views.interfaces.categories.ICategorizable;
import org.eclipse.tcf.te.ui.views.navigator.DelegatingLabelProvider;
import org.eclipse.ui.IPersistableElement;

/**
 * Adapter factory implementation.
 */
public class AdapterFactory implements IAdapterFactory {
	// The adapter for ILabelProvider.class
	private final DelegatingLabelProvider labelProvider = new DelegatingLabelProvider();
	// The adapter for IEditorSaveAsAdapter.class
	private final IEditorSaveAsAdapter editorSaveAsAdapter = new EditorSaveAsAdapter();

	// The adapter class.
	private Class<?>[] adapters = {
					ILabelProvider.class,
					IPersistableElement.class,
					ICategorizable.class,
					IPeerNode.class,
					IEditorSaveAsAdapter.class
	};

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof ILocatorNode) {
			if (ILabelProvider.class.equals(adapterType)) {
				return labelProvider;
			}
		}

		if (adaptableObject instanceof IPeer) {
			if (ILabelProvider.class.equals(adapterType)) {
				return labelProvider;
			}
		}

		if (adaptableObject instanceof IPeerNode) {
			if (ILabelProvider.class.equals(adapterType)) {
				return labelProvider;
			}
			if (IPersistableElement.class.equals(adapterType)) {
				return new PersistablePeerNode((IPeerNode)adaptableObject);
			}
			if (ICategorizable.class.equals(adapterType)) {
				return new CategorizableAdapter(adaptableObject);
			}

		}

		if (adaptableObject instanceof EditorInput) {
			if (IPeerNode.class.equals(adapterType)) {
				return ((EditorInput)adaptableObject).getAdapter(adapterType);
			}
			if (IEditorSaveAsAdapter.class.equals(adapterType)) {
				return editorSaveAsAdapter;
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
