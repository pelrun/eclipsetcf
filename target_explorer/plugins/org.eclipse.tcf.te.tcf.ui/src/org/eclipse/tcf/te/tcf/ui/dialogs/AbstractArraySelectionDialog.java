/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.dialogs;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.tcf.ui.navigator.ViewerSorter;
import org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.tcf.te.ui.views.navigator.DelegatingLabelProvider;

/**
 * Selection dialog implementation.
 */
public abstract class AbstractArraySelectionDialog extends CustomTitleAreaDialog {
	// The table viewer
	/* default */ TableViewer viewer;

	// The selection. Will be filled in if "OK" is pressed
	private ISelection selection;

	/**
	 * Constructor.
	 *
	 * @param parent The parent shell used to view the dialog, or <code>null</code>.
	 * @param services The list of (remote) services the agents must provide to be selectable, or <code>null</code>.
	 */
	public AbstractArraySelectionDialog(Shell shell, String contextHelpId) {
		super(shell, contextHelpId);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	@Override
	protected boolean isResizable() {
	    return true;
	}

	/**
	 * Returns whether the dialog shall support multi selection or not.
	 * <p>
	 * The default implementation returns <code>true</code>.
	 *
	 * @return <code>True</code> if multi selection is supported, <code>false</code> otherwise.
	 */
	protected boolean supportsMultiSelection() {
		return false;
	}

	/**
	 * Returns the table viewer instance.
	 *
	 * @return The table viewer instance or <code>null</code>.
	 */
	protected final TableViewer getViewer() {
		return viewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog#createDialogAreaContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createDialogAreaContent(Composite parent) {
	    super.createDialogAreaContent(parent);

		setDialogTitle(getDialogTitle());
		setTitle(getTitle());
		setDefaultMessage(getDefaultMessage(), IMessageProvider.NONE);

		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		comp.setLayout(gl);
	    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
	    comp.setLayoutData(gd);

		createTableAreaContent(comp);
		createButtonAreaContent(comp);
	}

	protected int getTableViewerStyle() {
		return (supportsMultiSelection() ? SWT.MULTI : SWT.SINGLE) | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL;
	}

	protected TableViewer createTableAreaContent(Composite parent) {
		// Create the tree viewer
		viewer = new TableViewer(parent, getTableViewerStyle());

	    // Configure the table
	    final Table table = viewer.getTable();
		TableLayout tl = new TableLayout();
		tl.addColumnData(new ColumnWeightData(100));
		table.setLayout(tl);
	    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
	    table.setLayoutData(gd);

        final TableColumn column = new TableColumn(table, SWT.LEFT);

		ColumnViewerToolTipSupport.enableFor(viewer);


	    table.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				column.setWidth(Math.max(200, table.getBounds().width - table.getBorderWidth()*2));
			}
		});

	    viewer.setContentProvider(getContentProvider());
	    viewer.setLabelProvider(getLabelProvider());

	    // Subclasses may customize the viewer before setting the input
	    configureTableViewer(viewer);

	    viewer.setInput(getInput());
	    viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				onDoubleClick();
			}
		});
	    viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateEnablement(viewer);
			}
		});

	    viewer.refresh();
	    updateSelection(getSelection());

	    return viewer;
	}

	protected void onDoubleClick() {
		if (!viewer.getSelection().isEmpty()) {
			okPressed();
		}
	}

	protected abstract void updateSelection(ISelection selection);

	protected IContentProvider getContentProvider() {
		return new ArrayContentProvider();
	}

	protected IBaseLabelProvider getLabelProvider() {
	    DelegatingLabelProvider labelProvider = new DelegatingLabelProvider();
	    return new DecoratingLabelProvider(labelProvider, labelProvider);
	}

	protected void refresh() {
		ExecutorsUtil.executeInUI(new Runnable() {
			@Override
			public void run() {
			    viewer.setInput(getInput());
			    viewer.refresh();
				updateEnablement(viewer);
			}
		});
	}

	protected abstract Object getInput();

	protected abstract void createButtonAreaContent(Composite parent);

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TrayDialog#createButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createButtonBar(Composite parent) {
	    Control buttonBar = super.createButtonBar(parent);
	    updateEnablement(viewer);
	    return buttonBar;
	}

	/**
	 * Update the enablement of the dialog widgets.
	 *
	 * @param viewer The table viewer. Must not be <code>null</code>.
	 */
	protected void updateEnablement(TableViewer viewer) {
		Assert.isNotNull(viewer);

		if (viewer.getTable().getSelectionCount() == 0) {
			viewer.getTable().setSelection(0);
		}

	    // Adjust the OK button enablement
	    Button okButton = getButton(IDialogConstants.OK_ID);
	    int selCount = viewer.getTable().getSelectionCount();
	    boolean selOk = supportsMultiSelection() ? selCount > 0 : selCount == 1;
	    SWTControlUtil.setEnabled(okButton, viewer.getTable().getItems().length > 0 && selOk);
	}

	/**
	 * Configure the table viewer.
	 * <p>
	 * The default implementation does nothing. Subclasses may overwrite this
	 * method to customize the table viewer before the input gets set.
	 *
	 * @param viewer The table viewer. Must not be <code>null</code>.
	 */
	protected void configureTableViewer(TableViewer viewer) {
		Assert.isNotNull(viewer);
        viewer.setSorter(new ViewerSorter());
	}

	/**
	 * Returns the dialog title.
	 *
	 * @return The dialog title.
	 */
	protected abstract String getDialogTitle();

	/**
	 * Returns the title.
	 *
	 * @return The title.
	 */
	protected abstract String getTitle();

	/**
	 * Returns the default message.
	 *
	 * @return The default message.
	 */
	protected abstract String getDefaultMessage();

	/**
	 * Returns the selection which had been set to the viewer at
	 * the time of closing the dialog with either "OK" or "Cancel".
	 *
	 * @return The selection or <code>null</code>.
	 */
	public ISelection getSelection() {
		return selection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		selection = viewer.getSelection();
	    super.okPressed();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#cancelPressed()
	 */
	@Override
	protected void cancelPressed() {
		selection = null;
	    super.cancelPressed();
	}
}
