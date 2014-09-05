/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.internal.preferences;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.tcf.te.ui.terminals.nls.Messages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Terminals top preference page implementation.
 */
public class PreferencePage extends org.eclipse.jface.preference.PreferencePage implements IWorkbenchPreferencePage {
	private TableViewer viewer;

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
		GridData layoutData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		panel.setLayoutData(layoutData);

		Label label = new Label(panel, SWT.HORIZONTAL);
		label.setText(Messages.PreferencePage_label);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Group group = new Group(panel, SWT.NONE);
		group.setText(Messages.PreferencePage_executables_label);
		group.setLayout(new GridLayout(2, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		viewer = new TableViewer(group);

		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

        TableColumn column = new TableColumn(table, SWT.LEFT);
        column.setText(Messages.PreferencePage_executables_column_name_label);
        column = new TableColumn(table, SWT.LEFT);
        column.setText(Messages.PreferencePage_executables_column_path_label);

		ColumnViewerToolTipSupport.enableFor(viewer);

		TableLayout tableLayout = new TableLayout();
		tableLayout.addColumnData(new ColumnWeightData(35));
		tableLayout.addColumnData(new ColumnWeightData(65));
		table.setLayout(tableLayout);

		layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		layoutData.heightHint = SWTControlUtil.convertHeightInCharsToPixels(table, 10);
		table.setLayoutData(layoutData);

		Composite buttonsPanel = new Composite(group, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0; layout.marginWidth = 0;
		buttonsPanel.setLayout(layout);
		buttonsPanel.setLayoutData(new GridData(SWT.LEAD, SWT.BEGINNING, false, false));

		Button button = new Button(buttonsPanel, SWT.PUSH);
		button.setText(Messages.PreferencePage_executables_button_add_label);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, false, false);
		layoutData.widthHint = SWTControlUtil.convertWidthInCharsToPixels(button, 10);
		button.setLayoutData(layoutData);

		button = new Button(buttonsPanel, SWT.PUSH);
		button.setText(Messages.PreferencePage_executables_button_edit_label);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		layoutData.widthHint = SWTControlUtil.convertWidthInCharsToPixels(button, 10);
		button.setLayoutData(layoutData);

		button = new Button(buttonsPanel, SWT.PUSH);
		button.setText(Messages.PreferencePage_executables_button_remove_label);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		layoutData.widthHint = SWTControlUtil.convertWidthInCharsToPixels(button, 10);
		button.setLayoutData(layoutData);

		return panel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
	    super.performDefaults();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
	    return super.performOk();
	}

}
