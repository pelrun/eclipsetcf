/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.preferences;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.tcf.te.ui.activator.UIPlugin;
import org.eclipse.tcf.te.ui.interfaces.IPreferenceKeys;
import org.eclipse.tcf.te.ui.jface.dialogs.OptionalMessageDialog;
import org.eclipse.tcf.te.ui.nls.Messages;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Top preference page implementation.
 */
public class PreferencePage extends org.eclipse.jface.preference.PreferencePage implements IWorkbenchPreferencePage {
	private Button resetDoNotShowAgainMarkers;
	private Button persistEditors = null;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		Label label = new Label(panel, SWT.HORIZONTAL);
		label.setText(Messages.PreferencePage_label);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Group group = new Group(panel, SWT.NONE);
		group.setText(Messages.PreferencePage_sessions_label);
		group.setLayout(new GridLayout());
		group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		persistEditors = new Button(group, SWT.CHECK);
		persistEditors.setText(Messages.PreferencePage_persistEditors_label);
		persistEditors.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		persistEditors.setSelection(UIPlugin.getScopedPreferences().getBoolean(IPreferenceKeys.PREF_PERSIST_EDITORS));

		group = new Group(panel, SWT.NONE);
		group.setText(Messages.PreferencePage_dialogs_label);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		label = new Label(group, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		label.setText(Messages.PreferencePage_resetDoNotShowAgainMarkers_message);

		resetDoNotShowAgainMarkers = new Button(group, SWT.PUSH);
		resetDoNotShowAgainMarkers.setText(Messages.PreferencePage_resetDoNotShowAgainMarkers_label);
		GridData layoutData = new GridData(GridData.END, GridData.CENTER, false, false);
		layoutData.widthHint = SWTControlUtil.convertWidthInCharsToPixels(resetDoNotShowAgainMarkers, resetDoNotShowAgainMarkers.getText().length() + 4);
		resetDoNotShowAgainMarkers.setLayoutData(layoutData);
		resetDoNotShowAgainMarkers.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				OptionalMessageDialog.clearAllRememberedStates();

				MessageDialog.openInformation(getShell(),
						Messages.PreferencePage_resetDoNotShowAgainMarkers_dialog_title,
						Messages.PreferencePage_resetDoNotShowAgainMarkers_dialog_message);
			}
		});

		return panel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		if (persistEditors != null) {
			persistEditors.setSelection(UIPlugin.getScopedPreferences().getDefaultBoolean(IPreferenceKeys.PREF_PERSIST_EDITORS));
		}
	    super.performDefaults();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		if (persistEditors != null) {
			UIPlugin.getScopedPreferences().putBoolean(IPreferenceKeys.PREF_PERSIST_EDITORS, persistEditors.getSelection());
		}
	    return super.performOk();
	}

}
