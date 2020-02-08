/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.search;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;
import org.eclipse.tcf.te.ui.controls.BaseEditBrowseTextControl;
import org.eclipse.tcf.te.ui.search.TreeViewerSearchDialog;

/**
 * The searchable that provides a UI to collect and test
 * the user of a process during searching.
 */
public class ProcessUserSearchable extends ProcessBaseSearchable {
	// Constant values of user options
	private static final int OPTION_NOT_REMEMBER = 0;
	private static final int OPTION_BY_ME = 1;
	private static final int OPTION_SPECIFIED = 2;

	// The choice selected
	private int choice;
	// The specified user when "Specify user" is selected.
	private String user;

	// UI elements to input
	private Button fBtnUserNotRem;
	private Button fBtnUserMe;
	private Button fBtnUserSpecified;
	private BaseEditBrowseTextControl txtUser;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.utils.AbstractSearchable#createAdvancedPart(org.eclipse.tcf.te.ui.search.TreeViewerSearchDialog, org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createAdvancedPart(TreeViewerSearchDialog dialog, Composite parent) {
		SelectionListener l = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				optionChecked(e);
			}
		};
		Composite modifiedComp = createGroup(parent);
		modifiedComp.setLayout(new GridLayout(2, false));

		fBtnUserNotRem = new Button(modifiedComp, SWT.RADIO);
		fBtnUserNotRem.setText(Messages.ProcessUserSearchable_DontRemember);
		fBtnUserNotRem.setSelection(true);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		fBtnUserNotRem.setLayoutData(data);
		fBtnUserNotRem.addSelectionListener(l);

		fBtnUserMe = new Button(modifiedComp, SWT.RADIO);
		fBtnUserMe.setText(Messages.ProcessUserSearchable_Myself);
		data = new GridData();
		data.horizontalSpan = 2;
		fBtnUserMe.setLayoutData(data);
		fBtnUserMe.addSelectionListener(l);

		fBtnUserSpecified = new Button(modifiedComp, SWT.RADIO);
		fBtnUserSpecified.setText(Messages.ProcessUserSearchable_SpecifyUser);
		data = new GridData();
		fBtnUserSpecified.setLayoutData(data);
		fBtnUserSpecified.addSelectionListener(l);

		Composite cmpUser = new Composite(modifiedComp, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		cmpUser.setLayout(layout);
		data = new GridData();
		cmpUser.setLayoutData(data);

		txtUser = new BaseEditBrowseTextControl(null);
		txtUser.setIsGroup(false);
		txtUser.setHasHistory(false);
		txtUser.setHideBrowseButton(true);
		txtUser.setParentControlIsInnerPanel(true);
		txtUser.setupPanel(cmpUser);
		txtUser.setEnabled(false);
		Text text = (Text) txtUser.getEditFieldControl();
		text.addModifyListener(new ModifyListener() {
            @Override
			public void modifyText(ModifyEvent e) {
				userModified();
			}
		});
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.processes.ui.search.ProcessBaseSearchable#getSectionTitle()
	 */
	@Override
	protected String getGroupTitle() {
	    return Messages.ProcessUserSearchable_WhoStarted;
	}

	/**
	 * The modified event of the user fields.
	 */
	protected void userModified() {
		fireOptionChanged();
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.utils.AbstractSearchable#isInputValid()
	 */
	@Override
    public boolean isInputValid() {
		if(choice == OPTION_SPECIFIED && txtUser != null) {
			boolean vFrom = txtUser.isValid();
			if(vFrom) {
				String fromText = txtUser.getEditFieldControlText().trim();
				this.user = fromText;
			}
			return vFrom;
		}
	    return true;
    }

	/**
	 * The method handling the selection event.
	 *
	 * @param e The selection event.
	 */
	protected void optionChecked(SelectionEvent e) {
		Object src = e.getSource();
		boolean specified = false;
		if(src == fBtnUserNotRem) {
			choice = OPTION_NOT_REMEMBER;
		}
		else if(src == fBtnUserMe) {
			choice = OPTION_BY_ME;
		}
		else if(src == fBtnUserSpecified) {
			choice = OPTION_SPECIFIED;
			specified = true;
		}
		if (txtUser != null) {
			txtUser.setEnabled(specified);
		}
		fireOptionChanged();
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.ISearchMatcher#match(java.lang.Object)
	 */
	@Override
    public boolean match(Object element) {
		if (element instanceof IProcessContextNode) {
			final IProcessContextNode node = (IProcessContextNode) element;

			final AtomicReference<String> username = new AtomicReference<String>();
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					username.set(node.getSysMonitorContext().getUserName());
				}
			};

			Assert.isTrue(!Protocol.isDispatchThread());
			Protocol.invokeAndWait(runnable);

			switch (choice) {
			case OPTION_NOT_REMEMBER:
				return true;
			case OPTION_BY_ME:
				return System.getProperty("user.name").equals(username.get()); //$NON-NLS-1$
			case OPTION_SPECIFIED:
				return user == null ? username.get() == null : user.equals(username.get());
			}
		}
		return false;
    }
}
