/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.adapters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.columns.FSTreeElementLabelProvider;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.search.FSTreeNodeSearchable;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;
import org.eclipse.tcf.te.ui.interfaces.ILazyLoader;
import org.eclipse.tcf.te.ui.interfaces.ISearchable;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.IPersistableElement;

/**
 * The adapter factory of <code>IFSTreeNode</code> over <code>IActionFilter</code>
 */
public class FSTreeNodeAdapterFactory implements IAdapterFactory {
	private static ILabelProvider nodeLabelProvider = new FSTreeElementLabelProvider();
	// The fFilters map caching fFilters for FS nodes.
	private Map<IFSTreeNode, NodeStateFilter> filters;

	public static class FSTreeNodePeerNodeProvider extends PlatformObject implements IPeerNodeProvider {
		private final IFSTreeNode node;

		/**
		 * Constructor
		 */
		public FSTreeNodePeerNodeProvider(IFSTreeNode node) {
			Assert.isNotNull(node);
			this.node = node;
		}

		/**
		 * Returns the associated file system tree node.
		 *
		 * @return The associated file system tree node.
		 */
		public final IFSTreeNode getFSTreeNode() {
			return node;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider#getPeerModel()
		 */
		@Override
		public final IPeerNode getPeerNode() {
			return node.getPeerNode();
		}
	}

	/**
	 * Constructor.
	 */
	public FSTreeNodeAdapterFactory() {
		this.filters = Collections.synchronizedMap(new HashMap<IFSTreeNode, NodeStateFilter>());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adaptableObject instanceof IFSTreeNode) {
			IFSTreeNode node = (IFSTreeNode) adaptableObject;
			if (adapterType == IActionFilter.class) {
				NodeStateFilter filter = filters.get(node);
				if (filter == null) {
					filter = new NodeStateFilter(node);
					filters.put(node, filter);
				}
				return filter;
			}
			else if (adapterType == ILabelProvider.class) {
				return nodeLabelProvider;
			}
			else if (adapterType == ILazyLoader.class) {
				return new FSTreeNodeLoader(node);
			}
			else if (adapterType == IPeerNodeProvider.class) {
				return new FSTreeNodePeerNodeProvider(node);
			}
			else if (adapterType == IPeerNode.class) {
				return new FSTreeNodePeerNodeProvider(node).getPeerNode();
			}
			else if (adapterType == ISearchable.class) {
				return new FSTreeNodeSearchable(node);
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
	 */
	@Override
	public Class[] getAdapterList() {
		return new Class[] { IActionFilter.class, ILabelProvider.class, IPersistableElement.class, ILazyLoader.class, ISearchable.class, IPeerNodeProvider.class };
	}
}
