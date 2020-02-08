/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.filetransfer;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.launch.ui.tabs.filetransfers.AbstractFileTransferSection;
import org.eclipse.tcf.te.launch.ui.tabs.filetransfers.AbstractFileTransferTab;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;

/**
 * File transfer launch configuration tab implementation.
 */
public class FileTransferTab extends AbstractFileTransferTab {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.tabs.filetransfers.AbstractFileTransferTab#createFileTransferSection(org.eclipse.ui.forms.IManagedForm, org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected AbstractFileTransferSection createFileTransferSection(IManagedForm form, Composite panel) {
		return new FileTransferSection(getManagedForm(), panel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
	 */
	@Override
	public Image getImage() {
		return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_COPY);
	}
}
