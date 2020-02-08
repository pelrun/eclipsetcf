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

import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IWindowsFileAttributes;

public interface IFSTreeNodeBase {
	enum Type {
		FILE_SYSTEM, ROOT, DIRECTORY_OR_FILE
	}

	/**
	 * Returns the name of the tree node.
	 */
	String getName();

	/**
	 * Returns the type of the tree node.
	 */
	Type getType();

	/**
	 * Returns the location of the file on the remote system
	 */
	String getLocation();

	/**
	 * Returns a label for the type of the file or directory
	 */
	String getFileTypeLabel();

	/**
	 * Returns information about the user account under which the remote agent accesses
	 * the file system
	 */
    IUserAccount getUserAccount();

    /**
     * Creates a working copy for the purpose of changing permissions
     */
	IFSTreeNodeWorkingCopy createWorkingCopy();

	/**
	 * Returns whether this node represents the entire file system
	 */
	boolean isFileSystem();

	/**
	 * Returns whether this node represents a root directory like a disk
	 */
	boolean isRootDirectory();

	/**
	 * Returns whether this node represents a directory. Root directories are
	 * also considered to be directories.
	 */
	boolean isDirectory();

	/**
	 * Returns whether this node represents a file.
	 */
	boolean isFile();

	/**
	 * Returns the time of the last access
	 */
	long getAccessTime();

	/**
	 * Returns the time of the last modification
	 */
	long getModificationTime();

	/**
	 * Returns the size of the file or <code>0</code>, if not applicable.
	 */
	long getSize();

	/**
	 * Returns whether the file is marked as read only (Windows feature)
	 */
	boolean isReadOnly();

	/**
	 * Checks whether the agent has read permissions for the file
	 */
	boolean isReadable();

	/**
	 * Checks whether the agent has write permissions for the file
	 */
	boolean isWritable();

	/**
	 * Checks whether the agent has execute permissions for the file
	 */
	boolean isExecutable();

	/**
	 * Checks whether the file is marked as a system file (Windows feature)
	 */
	boolean isSystemFile();

	/**
	 * Checks whether the file is marked as a hidden file (Windows feature)
	 */
	boolean isHidden();

	/**
	 * Checks whether the agent is the owner of the file
	 */
	boolean isAgentOwner();

	/**
	 * Checks whether the node represents a windows file entity
	 */
	boolean isWindowsNode();

	/**
	 * Checks for the given permissions.
	 * @param bit a combination of IFileSystem.S_ values
	 */
	boolean getPermission(int bit);

	/**
	 * Checks for the given windows attribute.
	 * @param attribute a combination of attributes defined at {@link IWindowsFileAttributes}
	 */
	boolean getWin32Attr(int attribute);

	/**
	 * Returns the user id for this file
	 */
	int getUID();

	/**
	 * Returns the group id for this file
	 */
	int getGID();
}
