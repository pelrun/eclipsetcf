/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.controls;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.core.interfaces.IPropertyChangeProvider;
import org.eclipse.tcf.te.runtime.model.MessageModelNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.ui.trees.CommonViewerListener;
import org.eclipse.tcf.te.ui.trees.Pending;

/**
 * The base tree content provider that defines several default methods.
 */
public abstract class TreeContentProvider implements ITreeContentProvider {

	/**
	 * Static reference to the return value representing no elements.
	 */
	protected final static Object[] NO_ELEMENTS = new Object[0];

	// The listener to refresh the common viewer when properties change.
	protected CommonViewerListener commonViewerListener;
	// The viewer inputs that have been added a property change listener.
	private Set<IPropertyChangeProvider> providers = Collections.synchronizedSet(new HashSet<IPropertyChangeProvider>());
	// The viewer
	protected TreeViewer viewer;
	// The pending nodes and their direct parents.
	private Map<Object, Pending> pendings;

	// The target's peer model.
	private IPeerNode peerNode;

	/**
	 * Create a tree content provider.
	 */
	public TreeContentProvider() {
		pendings = new HashMap<Object, Pending>();
	}

	/**
	 * Get the pending node for the specified parent.
	 * If it exists, then return it. If not, create one
	 * and save it and return it.
	 */
	protected Pending getPending(Object parent) {
		Pending pending = pendings.get(parent);
		if(pending == null && viewer != null) {
			pending = new Pending(viewer);
			pendings.put(parent, pending);
		}
		return pending;
	}


	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		for(IPropertyChangeProvider provider : providers) {
			provider.removePropertyChangeListener(commonViewerListener);
		}
		commonViewerListener.cancel();
		providers.clear();
		pendings.clear();
	}

	/**
	 * Get the filtered children of the parent using the
	 * filters registered in the viewer.
	 *
	 * @param parent The parent element.
	 * @return The children after filtering.
	 */
	private Object[] getFilteredChildren(Object parent) {
		Object[] result = getChildren(parent);
		if (viewer != null) {
			ViewerFilter[] filters = viewer.getFilters();
			if (filters != null) {
				for (ViewerFilter filter : filters) {
					Object[] filteredResult = filter.filter(viewer, parent, result);
					result = filteredResult;
				}
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		Assert.isNotNull(parentElement);

		if (parentElement instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) parentElement;
			IPropertyChangeProvider provider = (IPropertyChangeProvider) adaptable.getAdapter(IPropertyChangeProvider.class);
			if (provider != null) {
				installPropertyChangeListener(provider);
			}
		}

		return NO_ELEMENTS;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (commonViewerListener != null) {
			for (IPropertyChangeProvider provider : providers) {
				provider.removePropertyChangeListener(commonViewerListener);
			}
		}

		Assert.isTrue(viewer instanceof TreeViewer);
		this.viewer = (TreeViewer) viewer;
		this.commonViewerListener = new CommonViewerListener(this.viewer, this);
		peerNode = getPeerNode(newInput);

		for (IPropertyChangeProvider provider : providers) {
			provider.addPropertyChangeListener(commonViewerListener);
		}
	}

    protected IPeerNode getPeerNode(Object input) {
		IPeerNode peerNode = input instanceof IPeerNode ? (IPeerNode)input : null;
		if (peerNode == null && input instanceof IAdaptable) {
			peerNode = (IPeerNode)((IAdaptable)input).getAdapter(IPeerNode.class);
		}
		return peerNode;
    }

	/**
	 * Install a property change listener to the specified element.
	 *
	 * @param provider The element node.
	 */
	private void installPropertyChangeListener(IPropertyChangeProvider provider) {
		if (provider != null && !providers.contains(provider)) {
			if (commonViewerListener != null) {
				provider.addPropertyChangeListener(commonViewerListener);
			}
			providers.add(provider);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		Object[] children = getFilteredChildren(element);
		return children != null && children.length > 0;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (peerNode != null && peerNode.getConnectState() == IConnectable.STATE_CONNECTED) {
	        return getChildren(inputElement);
		}

		String message = null;
		if (peerNode != null) {
			if (peerNode.getConnectState() == IConnectable.STATE_CONNECTION_LOST ||
						peerNode.getConnectState() == IConnectable.STATE_CONNECTION_RECOVERING) {
				message = Messages.getStringDelegated(peerNode, "FileSystem_ContentProvider_connectionLost"); //$NON-NLS-1$
			}
			if (message == null) {
				message = Messages.getStringDelegated(peerNode, "FileSystem_ContentProvider_notConnected"); //$NON-NLS-1$
			}
		}

		return new Object[] { new MessageModelNode(message != null ? message : Messages.ContentProvider_notConnected, IStatus.INFO, false) };
	}
}
