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

import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IWindowsFileAttributes;


public interface IFSTreeNodeWorkingCopy extends IFSTreeNodeBase {

	/**
	 * Sets the write permission depending on whether the agent is owner, group member or
	 * neither.
	 */
	void setWritable(boolean value);

	/**
	 * Sets a permission as defined in {@link IFileSystem}
	 */
	void setPermission(int bit, boolean value);

	/**
	 * Changes the read only attribute (windows feature)
	 */
	void setReadOnly(boolean value);

	/**
	 * Changes the hidden attribute (windows feature)
	 */
	void setHidden(boolean selection);

	/**
	 * Sets a windows attribute as defined in {@link IWindowsFileAttributes}.
	 */
	void setWin32Attr(int bit, boolean value);

	/**
	 * Checks whether the working copy is dirty with respect to the original node
	 */
	boolean isDirty();

	/**
	 * Returns an operation for applying the changes
	 */
	IOperation operationCommit();
}
