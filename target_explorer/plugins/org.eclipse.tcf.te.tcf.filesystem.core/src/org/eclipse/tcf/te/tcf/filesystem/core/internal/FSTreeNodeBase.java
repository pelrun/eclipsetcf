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
package org.eclipse.tcf.te.tcf.filesystem.core.internal;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IWindowsFileAttributes;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNodeBase;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IUserAccount;

/**
 * Representation of a file system tree node.
 * <p>
 * <b>Note:</b> Node construction and child list access is limited to the TCF
 * event dispatch thread.
 */
public abstract class FSTreeNodeBase extends PlatformObject implements IFSTreeNodeBase {

	protected abstract int getWin32Attrs();
	protected abstract int getPermissions();
	protected abstract boolean checkPermission(int user, int group, int other);

	@Override
    public final boolean getWin32Attr(int attribute) {
    	return (getWin32Attrs() & attribute) == attribute;
    }

	@Override
	public final boolean isHidden() {
        return getWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_HIDDEN);
    }

    @Override
	public final boolean isReadOnly() {
        return getWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_READONLY);
    }

	@Override
	public final boolean getPermission(int bit) {
		return (getPermissions() & bit) == bit;
	}


    @Override
	public final boolean isReadable() {
    	return checkPermission(IFileSystem.S_IRUSR, IFileSystem.S_IRGRP, IFileSystem.S_IROTH);
    }

    @Override
	public final boolean isWritable() {
    	return checkPermission(IFileSystem.S_IWUSR, IFileSystem.S_IWGRP, IFileSystem.S_IWOTH);
    }

	@Override
	public final boolean isExecutable() {
    	return checkPermission(IFileSystem.S_IXUSR, IFileSystem.S_IXGRP, IFileSystem.S_IXOTH);
	}

	@Override
	public final boolean isAgentOwner() {
        IUserAccount account = getUserAccount();
        if (account != null) {
            return getUID() == account.getEUID();
        }
        return false;
    }

    @Override
	public final boolean isSystemFile() {
    	if (isFileSystem())
    		return false;

        return isWindowsNode() && getWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_SYSTEM);
    }
}
