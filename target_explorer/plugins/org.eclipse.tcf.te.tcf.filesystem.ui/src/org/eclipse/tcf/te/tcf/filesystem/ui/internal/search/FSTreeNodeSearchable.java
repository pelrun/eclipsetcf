/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.search;

import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.tcf.te.ui.utils.CompositeSearchable;

/**
 * The ISearchable adapter for a IFSTreeNode which creates a UI for the user to
 * input the matching condition and returns a matcher to do the matching.
 */
public class FSTreeNodeSearchable extends CompositeSearchable {

	/**
	 * Create an instance with the specified node.
	 *
	 * @param node The directory node.
	 */
	public FSTreeNodeSearchable(IFSTreeNode node) {
		super();
		setSearchables(new FSGeneralSearchable(node), new FSModifiedSearchable(), new FSSizeSearchable());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getSearchTitle(java.lang.Object)
	 */
	@Override
	public String getSearchTitle(Object rootElement) {
	    return Messages.FSTreeNodeSearchable_FindFilesAndFolders;
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getSearchMessage(java.lang.Object)
	 */
	@Override
    public String getSearchMessage(Object rootElement) {
		String message = Messages.FSTreeNodeSearchable_FindMessage;
		IFSTreeNode rootNode = (IFSTreeNode) rootElement;
		String rootName = getElementName(rootElement);
		if (rootNode != null && !rootNode.isFileSystem()) rootName = "\"" + rootName + "\""; //$NON-NLS-1$//$NON-NLS-2$
		message = NLS.bind(message, rootName);
		return message;
    }

	/**
	 * Get a name representation for each file node.
	 *
	 * @param rootElement The root element whose name is being retrieved.
	 * @return The node's name or an expression for the file system.
	 */
	private String getElementName(Object rootElement) {
		if(rootElement == null) {
			return Messages.FSTreeNodeSearchable_SelectedFileSystem;
		}
		IFSTreeNode rootNode = (IFSTreeNode) rootElement;
		if(rootNode.isFileSystem()) {
			return Messages.FSTreeNodeSearchable_SelectedFileSystem;
		}
		return rootNode.getName();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getElementText(java.lang.Object)
	 */
	@Override
    public String getElementText(Object element) {
	    return getElementName(element);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchable#getCustomMessage(java.lang.Object, java.lang.String)
	 */
	@Override
	public String getCustomMessage(Object rootElement, String key) {
	    return null;
	}
}
