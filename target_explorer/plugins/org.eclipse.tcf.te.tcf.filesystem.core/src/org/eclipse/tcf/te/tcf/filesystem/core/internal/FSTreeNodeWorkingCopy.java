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

import static java.text.MessageFormat.format;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tcf.services.IFileSystem;
import org.eclipse.tcf.services.IFileSystem.FileAttrs;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IWindowsFileAttributes;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNodeWorkingCopy;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IUserAccount;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.AbstractOperation;
import org.eclipse.tcf.te.tcf.filesystem.core.internal.operations.OpCommitAttr;
import org.eclipse.tcf.te.tcf.filesystem.core.nls.Messages;

class FSTreeNodeWorkingCopy extends FSTreeNodeBase implements IFSTreeNodeWorkingCopy {

	private final FSTreeNodeBase fOriginal;

	private int fPermissions;
    private int fWin32Attributes;


	FSTreeNodeWorkingCopy(FSTreeNodeWorkingCopy original) {
		fOriginal = original;
		fPermissions = original.fPermissions;
		fWin32Attributes = original.fWin32Attributes;
	}

	FSTreeNodeWorkingCopy(FSTreeNode original) {
		fOriginal = original;
		fPermissions = original.getPermissions();
		fWin32Attributes = original.getWin32Attrs();
	}

	@Override
	protected int getWin32Attrs() {
		return fWin32Attributes;
	}

	@Override
	protected int getPermissions() {
		return fPermissions;
	}

	@Override
	public IFSTreeNodeWorkingCopy createWorkingCopy() {
		return new FSTreeNodeWorkingCopy(this);
	}

	@Override
	public void setWritable(boolean b) {
        IUserAccount account = getUserAccount();
        if (account != null) {
            int bit;
            if (getUID() == account.getEUID()) {
                bit = IFileSystem.S_IWUSR;
            } else if (getGID() == account.getEGID()) {
                bit = IFileSystem.S_IWGRP;
            } else {
                bit = IFileSystem.S_IWOTH;
            }
            setPermission(bit, true);
        }
    }

    @Override
    public void setPermission(int bit, boolean value) {
    	if (value) {
    		fPermissions |= bit;
    	} else {
    		fPermissions &= ~bit;
    	}
    }

    @Override
    public void setWin32Attr(int bit, boolean value) {
    	if (value) {
    		fWin32Attributes |= bit;
    	} else {
    		fWin32Attributes &= ~bit;
    	}
    }

    @Override
	public void setHidden(boolean hidden) {
        setWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_HIDDEN, hidden);
    }

    @Override
	public void setReadOnly(boolean readOnly) {
        setWin32Attr(IWindowsFileAttributes.FILE_ATTRIBUTE_READONLY, readOnly);
    }

	@Override
	public boolean isDirty() {
		if (fOriginal.getPermissions() != getPermissions())
			return true;
		if (fOriginal.getWin32Attrs() != getWin32Attrs())
			return true;
		return false;
	}

	@Override
	public IOperation operationCommit() {
		return new AbstractOperation() {
			@Override
			public String getName() {
				return format(Messages.FSTreeNodeWorkingCopy_commitOperation_name, FSTreeNodeWorkingCopy.this.getName());
			}
			@Override
			protected IStatus doRun(IProgressMonitor monitor) {
				monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
				return doCommit(monitor);
			}
		};
	}

	protected IStatus doCommit(IProgressMonitor monitor) {
    	try {
    		if (fOriginal instanceof FSTreeNodeWorkingCopy) {
    			return commit((FSTreeNodeWorkingCopy) fOriginal);
    		}
    		if (fOriginal instanceof FSTreeNode) {
    			return commit((FSTreeNode) fOriginal, monitor);
    		}
    		return Status.CANCEL_STATUS;
    	} finally {
    		monitor.done();
    	}
    }

    private IStatus commit(FSTreeNodeWorkingCopy original) {
    	original.fPermissions = fPermissions;
    	original.fWin32Attributes = fWin32Attributes;
    	return Status.OK_STATUS;
    }


    private IStatus commit(FSTreeNode original, IProgressMonitor monitor) {
    	if (!isDirty())
    		return Status.OK_STATUS;

    	FileAttrs attrs = original.getAttributes();
    	if (attrs != null) {
    		Map<String, Object> attributes = new HashMap<String, Object>(attrs.attributes);
    		if (fWin32Attributes != original.getWin32Attrs()) {
    			attrs.attributes.put(FSTreeNode.KEY_WIN32_ATTRS, Integer.valueOf(fWin32Attributes));
    		}

    		attrs = new FileAttrs(attrs.flags, attrs.size, attrs.uid, attrs.gid, fPermissions, attrs.atime, attrs.mtime, attributes);
    		return new OpCommitAttr(original, attrs).run(monitor);
    	}
    	return Status.OK_STATUS;
    }

	@Override
	public String getName() {
		return fOriginal.getName();
	}

	@Override
	public Type getType() {
		return fOriginal.getType();
	}

	@Override
	public String getFileTypeLabel() {
		return fOriginal.getFileTypeLabel();
	}

	@Override
	public IUserAccount getUserAccount() {
		return fOriginal.getUserAccount();
	}

	@Override
	public String getLocation() {
		return fOriginal.getLocation();
	}

	@Override
	public boolean isFileSystem() {
		return fOriginal.isFileSystem();
	}

	@Override
	public boolean isRootDirectory() {
		return fOriginal.isRootDirectory();
	}

	@Override
	public boolean isDirectory() {
		return fOriginal.isDirectory();
	}

	@Override
	public boolean isFile() {
		return fOriginal.isFile();
	}

	@Override
	public long getAccessTime() {
		return fOriginal.getAccessTime();
	}

	@Override
	public long getModificationTime() {
		return fOriginal.getModificationTime();
	}

	@Override
	public long getSize() {
		return fOriginal.getSize();
	}

	@Override
	public boolean isWindowsNode() {
		return fOriginal.isWindowsNode();
	}

	@Override
	public int getUID() {
		return fOriginal.getUID();
	}

	@Override
	public int getGID() {
		return fOriginal.getGID();
	}

	@Override
	protected boolean checkPermission(int user, int group, int other) {
		return fOriginal.checkPermission(user, group, other);
	}
}
