/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.terminals.local.showin.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
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
import org.eclipse.tcf.te.ui.terminals.local.nls.Messages;
import org.eclipse.tcf.te.ui.terminals.local.showin.ExternalExecutablesDialog;
import org.eclipse.tcf.te.ui.terminals.local.showin.ExternalExecutablesManager;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Terminals top preference page implementation.
 */
public class PreferencePage extends org.eclipse.jface.preference.PreferencePage implements IWorkbenchPreferencePage {
	/* default */ TableViewer viewer;
	private Button addButton;
	private Button editButton;
	private Button removeButton;

	/* default */ final List<Map<String, Object>> executables = new ArrayList<Map<String, Object>>();

	/* default */ static final Object[] NO_ELEMENTS = new Object[0];

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

		viewer = new TableViewer(group, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);

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

		addButton = new Button(buttonsPanel, SWT.PUSH);
		addButton.setText(Messages.PreferencePage_executables_button_add_label);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, false, false);
		layoutData.widthHint = SWTControlUtil.convertWidthInCharsToPixels(addButton, 10);
		addButton.setLayoutData(layoutData);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ExternalExecutablesDialog dialog = new ExternalExecutablesDialog(PreferencePage.this.getShell(), false);
				if (dialog.open() == Window.OK) {
					// Get the executable properties and add it to the the list
					Map<String, Object> executableData = dialog.getExecutableData();
					if (executableData != null && !executables.contains(executableData)) {
						executables.add(executableData);
						viewer.refresh();
					}
				}
			}
		});

		editButton = new Button(buttonsPanel, SWT.PUSH);
		editButton.setText(Messages.PreferencePage_executables_button_edit_label);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		layoutData.widthHint = SWTControlUtil.convertWidthInCharsToPixels(editButton, 10);
		editButton.setLayoutData(layoutData);
		editButton.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("unchecked")
            @Override
			public void widgetSelected(SelectionEvent e) {
				ISelection s = viewer.getSelection();
				if (s instanceof IStructuredSelection && !s.isEmpty()) {
					Object element = ((IStructuredSelection)s).getFirstElement();
					if (element instanceof Map) {
						final Map<String, Object> m = (Map<String, Object>)element;
						ExternalExecutablesDialog dialog = new ExternalExecutablesDialog(PreferencePage.this.getShell(), true);
						dialog.setExecutableData(m);
						if (dialog.open() == Window.OK) {
							Map<String, Object> executableData = dialog.getExecutableData();
							if (executableData != null) {
								m.clear();
								m.putAll(executableData);
								viewer.refresh();
							}
						}
					}
				}
			}
		});

		removeButton = new Button(buttonsPanel, SWT.PUSH);
		removeButton.setText(Messages.PreferencePage_executables_button_remove_label);
		layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		layoutData.widthHint = SWTControlUtil.convertWidthInCharsToPixels(removeButton, 10);
		removeButton.setLayoutData(layoutData);

		viewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof List && !((List<?>)inputElement).isEmpty()) {
					return ((List<?>)inputElement).toArray();
				}
				return NO_ELEMENTS;
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			@Override
			public void dispose() {
			}
		});

		viewer.setLabelProvider(new ITableLabelProvider() {
			@SuppressWarnings("unchecked")
			@Override
			public String getColumnText(Object element, int columnIndex) {
				if (element instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>)element;

                    switch (columnIndex) {
                    case 0:
                    	return (String)m.get("Name"); //$NON-NLS-1$
                    case 1:
                    	return (String)m.get("Path"); //$NON-NLS-1$
                    }
				}
				return null;
			}

			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				return null;
			}

			@Override
			public void removeListener(ILabelProviderListener listener) {
			}

			@Override
			public boolean isLabelProperty(Object element, String property) {
				return false;
			}

			@Override
			public void dispose() {
			}

			@Override
			public void addListener(ILabelProviderListener listener) {
			}
		});

		List<Map<String, Object>> l = ExternalExecutablesManager.load();
		if (l != null) executables.addAll(l);

		viewer.setInput(executables);

		viewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});

		updateButtons();

		return panel;
	}

	/**
	 * Updates the button states.
	 */
	protected void updateButtons() {
		if (viewer != null) {
			SWTControlUtil.setEnabled(addButton, true);

			ISelection selection = viewer.getSelection();

			boolean hasSelection = selection != null && !selection.isEmpty();
			int count = selection instanceof IStructuredSelection ? ((IStructuredSelection)selection).size() : 0;

			SWTControlUtil.setEnabled(editButton, hasSelection && count == 1);
			SWTControlUtil.setEnabled(removeButton, hasSelection && count > 0);
		} else {
			SWTControlUtil.setEnabled(addButton, false);
			SWTControlUtil.setEnabled(editButton, false);
			SWTControlUtil.setEnabled(removeButton, false);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	protected void performDefaults() {
		executables.clear();
		List<Map<String, Object>> l = ExternalExecutablesManager.load();
		if (l != null) executables.addAll(l);
		viewer.refresh();

	    super.performDefaults();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		ExternalExecutablesManager.save(executables);
	    return super.performOk();
	}

}
