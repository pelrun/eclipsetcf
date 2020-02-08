/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.internal.adapters;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.runtime.IRuntimeModel;
import org.eclipse.tcf.te.tcf.processes.ui.navigator.runtime.LabelProviderDelegate;
import org.eclipse.tcf.te.tcf.processes.ui.search.ProcessSearchable;
import org.eclipse.tcf.te.ui.interfaces.ISearchable;

/**
 * Adapter factory implementation.
 */
public class AdapterFactory implements IAdapterFactory {
	// The adapter for ILabelProvider.class
	private ILabelProvider labelProvider = new LabelProviderDelegate();

	private static final Class<?>[] CLASSES = new Class[] {
		ILabelProvider.class,
		IPeerNode.class,
		ISearchable.class
	};

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object getAdapter(final Object adaptableObject, final Class adapterType) {
		if (adaptableObject instanceof IProcessContextNode) {
			if (ILabelProvider.class.equals(adapterType)) {
				return labelProvider;
			}
			if (IPeerNode.class.equals(adapterType)) {
				return ((IProcessContextNode) adaptableObject).getAdapter(adapterType);
			}
			if (ISearchable.class.equals(adapterType)) {
				return new ProcessSearchable((IPeerNode)((IProcessContextNode)adaptableObject).getAdapter(IPeerNode.class));
			}
		}

		if (adaptableObject instanceof IRuntimeModel) {
			if (ISearchable.class.equals(adapterType)) {
				final AtomicReference<IPeerNode> node = new AtomicReference<IPeerNode>();

				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						node.set(((IRuntimeModel)adaptableObject).getPeerNode());
					}
				};

				if (Protocol.isDispatchThread()) runnable.run();
				else Protocol.invokeAndWait(runnable);

				return new ProcessSearchable(node.get());
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
	 */
	@Override
	public Class[] getAdapterList() {
		return CLASSES;
	}

}
