/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.controls;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl;
import org.eclipse.tcf.te.ui.controls.validator.TextValidator;
import org.eclipse.tcf.te.ui.controls.validator.Validator;

/**
 * Peer name control implementation.
 */
public class PeerNameControl extends BaseEditBrowseTextControl {

	/**
	 * Constructor.
	 *
	 * @param parentPage The parent dialog page this control is embedded in.
	 *                   Might be <code>null</code> if the control is not associated with a page.
	 */
	public PeerNameControl(IDialogPage parentPage) {
		super(parentPage);

		setIsGroup(false);
		setHasHistory(false);
		setHideBrowseButton(true);
		setEditFieldLabel(Messages.PeerNameControl_label);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#doCreateEditFieldValidator()
	 */
	@Override
	protected Validator doCreateEditFieldValidator() {
	    return new TextValidator(Validator.ATTR_MANDATORY);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#configureEditFieldValidator(org.eclipse.tcf.te.ui.controls.validator.Validator)
	 */
	@Override
	protected void configureEditFieldValidator(Validator validator) {
		if (validator == null) return;
		validator.setMessageText(TextValidator.INFO_MISSING_NAME, Messages.PeerNameControl_Information_MissingName);
	}
}
