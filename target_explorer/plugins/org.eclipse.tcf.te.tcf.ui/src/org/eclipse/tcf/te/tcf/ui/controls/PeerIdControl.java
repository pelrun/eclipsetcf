/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.controls;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl;

/**
 * Peer id control implementation.
 */
public class PeerIdControl extends BaseEditBrowseTextControl {

	/**
	 * Constructor.
	 *
	 * @param parentPage The parent dialog page this control is embedded in.
	 *                   Might be <code>null</code> if the control is not associated with a page.
	 */
	public PeerIdControl(IDialogPage parentPage) {
		super(parentPage);

		setIsGroup(false);
		setHasHistory(false);
		setHideBrowseButton(true);
		setReadOnly(true);
		setEditFieldLabel(Messages.PeerIdControl_label);
	}

}
