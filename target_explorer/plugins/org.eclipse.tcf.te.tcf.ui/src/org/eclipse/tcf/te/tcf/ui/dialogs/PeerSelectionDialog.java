/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.dialogs;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;

/**
 * Peer selection dialog implementation.
 */
public class PeerSelectionDialog extends AbstractArraySelectionDialog {

	/**
	 * Constructor.
	 *
	 * @param shell The shell used to view the dialog, or <code>null</code>.
	 */
	public PeerSelectionDialog(Shell shell) {
		super(shell, IContextHelpIds.PEER_SELECTION_DIALOG);
	}

	@Override
    protected Object[] getInput() {
		return ModelManager.getLocatorModel().getPeers();
	}

	/**
	 * Returns the dialog title.
	 *
	 * @return The dialog title.
	 */
	@Override
    protected String getDialogTitle() {
		return Messages.PeerSelectionDialog_dialogTitle;
	}

	/**
	 * Returns the title.
	 *
	 * @return The title.
	 */
	@Override
    protected String getTitle() {
		return Messages.PeerSelectionDialog_title;
	}

	/**
	 * Returns the default message.
	 *
	 * @return The default message.
	 */
	@Override
    protected String getDefaultMessage() {
		return Messages.PeerSelectionDialog_message;
	}
}
