/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IResultOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.model.CacheState;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;

public interface IFSTreeNode extends IFSTreeNodeBase, IAdaptable {
	/**
	 * Returns the runtime model this node belongs to
	 */
	IRuntimeModel getRuntimeModel();

	/**
	 * Returns the peer node this node belongs to
	 */
	IPeerNode getPeerNode();

	/**
	 * Returns the parent node, or <code>null</code>
	 */
	IFSTreeNode getParent();

	/**
	 * Returns the child with the given name, or <code>null</code>.
	 */
	IFSTreeNode findChild(String name);

	/**
	 * Returns the children of this node, may be <code>null</code> in case the children
	 * have not been queried.
	 */
	IFSTreeNode[] getChildren();

	/**
	 * Returns an URL for this node
	 */
	URL getLocationURL();

	/**
	 * Returns an URI for this node
	 */
	URI getLocationURI();

	/**
	 * Return the last refresh time
	 */
	long getLastRefresh();

	/**
	 * Returns the current known cache state of this node.
	 */
	CacheState getCacheState();

	/**
	 * Returns the location of the local cache file, does not actually create the cache.
	 */
	File getCacheFile();

	/**
	 * Returns the preferred editor ID or <code>null</code>.
	 */
	String getPreferredEditorID();

	/**
	 * Stores a preferred editor id
	 */
	void setPreferredEditorID(String editorID);

	/**
	 * Returns the content type of the file, or <code>null</code> if not applicable
	 */
	IContentType getContentType();

	/**
	 * Checks whether the file is binary.
	 */
	boolean isBinaryFile();

	/**
	 * Checks whether this node is an ancestor of the argument
	 */
	boolean isAncestorOf(IFSTreeNode target);

	/**
	 * Returns an operation for refreshing the node
	 */
	IOperation operationRefresh(boolean recursive);

	/**
	 * Returns an operation for renaming the node
	 */
	IOperation operationRename(String newName);

	/**
	 * Returns an operation for uploading the content of the given file to the remote file
	 * represented by this node.
	 */
	IOperation operationUploadContent(File srcFile);

	/**
	 * Returns an operation to delete the remote file.
	 */
	IOperation operationDelete(IConfirmCallback readonlyCallback);

	/**
	 * Returns an operation for downloading the remote file to the given output stream
	 * @param output stream receiving the content of the remote file, or <code>null</code> for
	 * downloading it to the local cache.
	 */
	IOperation operationDownload(OutputStream output);

	/**
	 * Returns an operation for downloading the remote file or directory to the local file system.
	 * @param destinationFolder folder where to store the downloaded files and folders.
	 */
	IOperation operationDownload(File destinationFolder, IConfirmCallback confirmCallback);

	/**
	 * Returns an operation for uploading the given files into this remote directory
	 */
	IOperation operationDropFiles(List<String> files, IConfirmCallback confirmCallback);

	/**
	 * Returns an operation for moving the given remote files into this remote directory
	 */
	IOperation operationDropMove(List<IFSTreeNode> nodes, IConfirmCallback confirmCallback);

	/**
	 * Returns an operation for copying the given remote files into this remote directory
	 */
	IOperation operationDropCopy(List<IFSTreeNode> nodes, boolean cpPerm, boolean cpOwn, IConfirmCallback moveCopyCallback);

	/**
	 * Returns an operation for creating a new remote file
	 */
	IResultOperation<? extends IFSTreeNode> operationNewFile(String name);

	/**
	 * Returns an operation for creating a new remote folder
	 */
	IResultOperation<? extends IFSTreeNode> operationNewFolder(String name);

	/**
	 * Stores whether this file or folder shall be revealed when the target is connected.
	 */
	void setRevealOnConnect(boolean value);

	/**
	 * Returns whether this file or folder is revealed when the target is connected.
	 */
	boolean isRevealOnConnect();
}
