/**
 * FileTransferItemVariableDelegate.java
 * Created on 15.10.2012
 *
 * Copyright (c) 2012 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.launch.core.internal;

import org.eclipse.tcf.te.runtime.persistence.AbstractPathVariableDelegate;
import org.eclipse.tcf.te.runtime.services.interfaces.filetransfer.IFileTransferItem;

/**
 * FileTransferItemVariableDelegate
 */
public class FileTransferItemVariableDelegate extends AbstractPathVariableDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.AbstractVariableDelegate#getKeysToHandle()
	 */
	@Override
	protected String[] getKeysToHandle() {
		return new String[]{IFileTransferItem.PROPERTY_HOST};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.persistence.AbstractPathVariableDelegate#isPathKey(java.lang.String)
	 */
	@Override
	protected boolean isPathKey(String key) {
		return true;
	}
}
