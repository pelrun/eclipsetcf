/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.operations;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;

/**
 * Internal clip board transfer implementation used by the file system clip board.
 */
/* default */ class FsClipboardTransfer extends ByteArrayTransfer {

	private static final String TYPE_NAME= "fs-clipboard-transfer-format" + Long.toString(System.currentTimeMillis()); //$NON-NLS-1$;
	private static final int TYPEID= registerType(TYPE_NAME);

	private FsClipboard.FsClipboardContent content;

	private static class LazyInstance {
		public static FsClipboardTransfer instance = new FsClipboardTransfer();
	}

	/**
	 * Constructor
	 */
	/* default */ FsClipboardTransfer() {
	}

	/**
	 * Returns the singleton.
	 */
	public static FsClipboardTransfer getInstance() {
		return LazyInstance.instance;
	}

	/**
	 * Returns the transfer data.
	 *
	 * @return The transfer data or <code>null</code>.
	 */
	public FsClipboard.FsClipboardContent getContent() {
		return content;
	}

	/**
	 * Sets the transfer data.
	 *
	 * @param content The transfer data or <code>null</code>.
	 */
	public void setContent(FsClipboard.FsClipboardContent content) {
		this.content = content;
	}

    /* (non-Javadoc)
     * @see org.eclipse.swt.dnd.Transfer#getTypeIds()
     */
    @Override
	protected int[] getTypeIds() {
		return new int[] {TYPEID};
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.dnd.Transfer#getTypeNames()
     */
    @Override
	protected String[] getTypeNames() {
		return new String[] {TYPE_NAME};
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.dnd.Transfer#javaToNative(java.lang.Object, org.eclipse.swt.dnd.TransferData)
     */
    @Override
	protected void javaToNative(Object object, TransferData transferData) {
		byte[] check= TYPE_NAME.getBytes();
		super.javaToNative(check, transferData);
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.dnd.Transfer#nativeToJava(org.eclipse.swt.dnd.TransferData)
     */
    @Override
	protected Object nativeToJava(TransferData transferData) {
		Object result= super.nativeToJava(transferData);
		if (isInvalidNativeType(result)) {
            UIPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(), Messages.FsClipboardTransfer_errorMessage));
		}
		return content;
    }
	/**
	 * Tests whether native drop data matches this transfer type.
	 *
	 * @param result result of converting the native drop data to Java
	 * @return true if the native drop data does not match this transfer type.
	 * 	false otherwise.
	 */
	private boolean isInvalidNativeType(Object result) {
		return !(result instanceof byte[]) || !TYPE_NAME.equals(new String((byte[])result));
	}
}
