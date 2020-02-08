/*******************************************************************************
 * Copyright (c) 2012, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.handlers;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.ImageConsts;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.ui.PlatformUI;

/**
 * The confirmation callback implementation for operation "Move" and "Copy".
 */
public class MoveCopyCallback implements IConfirmCallback {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback#requires(java.lang.Object)
	 */
	@Override
	public boolean requires(Object object) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.filesystem.core.interfaces.IConfirmCallback#confirms(java.lang.Object)
	 */
	@Override
	public int confirms(Object object) {
		final boolean isDirectory = isDirectory(object);
		final String path = getAbsolutePath(object);
		final String name = getName(object);
		final int[] results = new int[1];
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				Shell parent = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				String title = isDirectory ? Messages.FSOperation_ConfirmFolderReplaceTitle : Messages.FSOperation_ConfirmFileReplace;
				String message = NLS.bind(isDirectory ? Messages.FSOperation_ConfirmFolderReplaceMessage : Messages.FSOperation_ConfirmFileReplaceMessage, path, name);
				final Image titleImage = UIPlugin.getImage(ImageConsts.REPLACE_FOLDER_CONFIRM);
				MessageDialog qDialog = new MessageDialog(parent, title, null, message, MessageDialog.QUESTION, new String[] { Messages.FSOperation_ConfirmDialogYes, Messages.FSOperation_ConfirmDialogYesToAll, Messages.FSOperation_ConfirmDialogNo, Messages.FSOperation_ConfirmDialogCancel }, 0) {
					@Override
					public Image getQuestionImage() {
						return titleImage;
					}
				};
				results[0] = qDialog.open();
			}
		});
		return results[0];
	}

	private String getName(Object object) {
		if (object instanceof IFSTreeNode)
			return ((IFSTreeNode) object).getName();
		if (object instanceof File)
			return ((File) object).getName();
	    return String.valueOf(object);
    }

	private String getAbsolutePath(Object object) {
		if (object instanceof IFSTreeNode)
			return ((IFSTreeNode) object).getLocation();
		if (object instanceof File)
			return ((File) object).getAbsolutePath();
	    return String.valueOf(object);
    }

	private boolean isDirectory(Object object) {
		if (object instanceof IFSTreeNode)
			return ((IFSTreeNode) object).isDirectory();
		if (object instanceof File)
			return ((File) object).isDirectory();
	    return false;
    }

}
