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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Tree viewer tree control delegating label provider.
 */
public class TreeViewerDelegatingContentProvider implements ITreeContentProvider {
	// The parent tree control instance
	private final AbstractTreeControl parentTreeControl;
	// The main content provider instance
	private final ITreeContentProvider contentProvider;

	/**
     * Constructor
     *
     * @param parentTreeControl The parent tree control instance. Must not be <code>null</code>.
     * @param contentProvider The main content provider instance. Must not be <code>null</code>.
     */
    public TreeViewerDelegatingContentProvider(AbstractTreeControl parentTreeControl, ITreeContentProvider contentProvider) {
    	super();

    	Assert.isNotNull(parentTreeControl);
    	this.parentTreeControl = parentTreeControl;
    	Assert.isNotNull(contentProvider);
    	this.contentProvider = contentProvider;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		boolean nonNullElements = false;
		List<Object> elements = new ArrayList<Object>();

		// Pass on to the main content provider
		Object[] candidates = contentProvider.getElements(inputElement);
		if (candidates != null) {
			nonNullElements = true;
			elements.addAll(Arrays.asList(candidates));
		}

		// Pass on to the delegate content providers
		ContentDescriptor[] descriptors = parentTreeControl.getContentDescriptors();
		if (descriptors != null) {
			for (ContentDescriptor descriptor : descriptors) {
				AbstractContentContribution contribution = descriptor.getContentContribution();
				if (contribution == null) continue;
				ITreeContentProvider delegate = contribution.getContentProvider();
				if (delegate == null) continue;
				candidates = delegate.getElements(inputElement);
				if (candidates != null) {
					nonNullElements = true;
					elements.addAll(Arrays.asList(candidates));
				}
			}
		}

		return nonNullElements ? elements.toArray() : null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		boolean nonNullChildren = false;
		List<Object> children = new ArrayList<Object>();

		// Pass on to the main content provider
		Object[] candidates = contentProvider.getChildren(parentElement);
		if (candidates != null) {
			nonNullChildren = true;
			children.addAll(Arrays.asList(candidates));
		}

		// Pass on to the delegate content providers
		ContentDescriptor[] descriptors = parentTreeControl.getContentDescriptors();
		if (descriptors != null) {
			for (ContentDescriptor descriptor : descriptors) {
				AbstractContentContribution contribution = descriptor.getContentContribution();
				if (contribution == null) continue;
				ITreeContentProvider delegate = contribution.getContentProvider();
				if (delegate == null) continue;
				candidates = delegate.getChildren(parentElement);
				if (candidates != null) {
					nonNullChildren = true;
					children.addAll(Arrays.asList(candidates));
				}
			}
		}

		return nonNullChildren ? children.toArray() : null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		// Pass on to the main content provider
		Object parent = contentProvider.getParent(element);
		if (parent == null) {
			// Pass on to the delegate content providers
			ContentDescriptor[] descriptors = parentTreeControl.getContentDescriptors();
			if (descriptors != null) {
				for (ContentDescriptor descriptor : descriptors) {
					AbstractContentContribution contribution = descriptor.getContentContribution();
					if (contribution == null) continue;
					if (contribution.isElementHandled(element)) {
						ITreeContentProvider delegate = contribution.getContentProvider();
						if (delegate != null) {
							parent = delegate.getParent(element);
							break;
						}
					}
				}
			}
		}
		return parent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		// Pass on to the main content provider
		boolean hasChildren = contentProvider.hasChildren(element);
		if (!hasChildren) {
			// Pass on to the delegate content providers
			ContentDescriptor[] descriptors = parentTreeControl.getContentDescriptors();
			if (descriptors != null) {
				for (ContentDescriptor descriptor : descriptors) {
					AbstractContentContribution contribution = descriptor.getContentContribution();
					if (contribution == null) continue;
					if (contribution.isElementHandled(element)) {
						ITreeContentProvider delegate = contribution.getContentProvider();
						if (delegate != null) {
							hasChildren = delegate.hasChildren(element);
							break;
						}
					}
				}
			}
		}
		return hasChildren;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		// Pass on to the main content provider
		contentProvider.dispose();
		// The delegate content providers are disposed via the content descriptors
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// Pass on to the main content provider
		contentProvider.inputChanged(viewer, oldInput, newInput);

		// Pass on to the delegate content providers
		ContentDescriptor[] descriptors = parentTreeControl.getContentDescriptors();
		if (descriptors != null) {
			for (ContentDescriptor descriptor : descriptors) {
				AbstractContentContribution contribution = descriptor.getContentContribution();
				if (contribution == null) continue;
				ITreeContentProvider delegate = contribution.getContentProvider();
				if (delegate == null) continue;
				delegate.inputChanged(viewer, oldInput, newInput);
			}
		}
	}
}
