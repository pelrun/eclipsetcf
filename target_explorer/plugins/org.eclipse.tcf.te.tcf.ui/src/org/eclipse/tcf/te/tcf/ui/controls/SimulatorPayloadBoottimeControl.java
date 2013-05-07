/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.controls;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl;
import org.eclipse.tcf.te.ui.controls.validator.NumberValidator;
import org.eclipse.tcf.te.ui.controls.validator.NumberVerifyListener;
import org.eclipse.tcf.te.ui.controls.validator.Validator;

/**
 * Simulator payload boot time control implementation.
 */
public class SimulatorPayloadBoottimeControl extends BaseEditBrowseTextControl {

	/**
	 * Constructor
	 *
	 * @param parentPage The parent dialog page this control is embedded in.
	 *                   Might be <code>null</code> if the control is not associated with a page.
	 */
	public SimulatorPayloadBoottimeControl(IDialogPage page) {
		super(page);

		setIsGroup(false);
		setEditFieldLabel(Messages.SimulatorPayloadBoottimeControl_label);
		setHasHistory(false);
		setHideBrowseButton(true);
		setLabelIsButton(true);
		setLabelButtonStyle(SWT.CHECK);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#doCreateEditFieldValidator()
	 */
	@Override
	protected Validator doCreateEditFieldValidator() {
	    return new NumberValidator();
	}

	private VerifyListener verifyListener;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl#doGetEditFieldControlVerifyListener()
	 */
	@Override
	protected VerifyListener doGetEditFieldControlVerifyListener() {
		if (verifyListener == null) {
			verifyListener = new NumberVerifyListener();
		}
	    return verifyListener;
	}

	/**
	 * Sets the simulator payload boot time in seconds.
	 *
	 * @param boottime The simulator payload boot time in seconds.
	 */
	public void setSimulatorPayloadBoottime(int boottime) {
		if (boottime > 0) {
			setEditFieldControlText(Integer.toString(boottime));
			setLabelControlSelection(true);
		} else {
			setEditFieldControlText(""); //$NON-NLS-1$
			setLabelControlSelection(false);
		}
	}

	/**
	 * Returns the simulator payload boot time in seconds.
	 *
	 * @return The simulator payload boot time in seconds.
	 */
	public int getSimulatorPayloadBoottime() {
		int boottime = 0;
		if (!"".equals(getEditFieldControlText())) { //$NON-NLS-1$
			boottime = Integer.decode(getEditFieldControlText()).intValue();
		}
		return boottime;
	}
}
