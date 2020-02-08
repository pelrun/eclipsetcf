/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
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
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.tcf.ui.navigator.ViewerSorter;
import org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.tcf.te.ui.trees.TreeArrayContentProvider;
import org.eclipse.tcf.te.ui.views.navigator.DelegatingLabelProvider;

/**
 * Tree selection dialog implementation.
 */
public abstract class AbstractTreeSelectionDialog extends CustomTitleAreaDialog {
	// The tree viewer
	/* default */ TreeViewer viewer;

	// The selection. Will be filled in if "OK" is pressed
	private ISelection selection;

	/**
	 * Constructor.
	 *
	 * @param parent The parent shell used to view the dialog, or <code>null</code>.
	 * @param services The list of (remote) services the agents must provide to be selectree, or <code>null</code>.
	 */
	public AbstractTreeSelectionDialog(Shell shell, String contextHelpId) {
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
	 * Returns the tree viewer instance.
	 *
	 * @return The tree viewer instance or <code>null</code>.
	 */
	protected final TreeViewer getViewer() {
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
	    GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
	    comp.setLayoutData(layoutData);

		createTreeAreaContent(comp);
		createButtonAreaContent(comp);
	}

	protected int getTreeViewerStyle() {
		return (supportsMultiSelection() ? SWT.MULTI : SWT.SINGLE) | SWT.BORDER;
	}

	protected TreeViewer createTreeAreaContent(Composite parent) {
	    // Create the tree viewer
	    viewer = new TreeViewer(parent, getTreeViewerStyle());

	    // Configure the tree
	    final Tree tree = viewer.getTree();

		@SuppressWarnings("unused")
		TreeColumn column = new TreeColumn(tree, SWT.LEFT);

		ColumnViewerToolTipSupport.enableFor(viewer);

		TableLayout treeLayout = new TableLayout();
		treeLayout.addColumnData(new ColumnWeightData(100));
		tree.setLayout(treeLayout);

	    GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
	    layoutData.minimumHeight = 150;
	    layoutData.minimumWidth = 200;
	    tree.setLayoutData(layoutData);

	    viewer.setContentProvider(getContentProvider());
	    viewer.setLabelProvider(getLabelProvider());

	    // Subclasses may customize the viewer before setting the input
	    configureTreeViewer(viewer);

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

	protected abstract void createButtonAreaContent(Composite parent);

	protected IContentProvider getContentProvider() {
		return new TreeArrayContentProvider();
	}

	protected IBaseLabelProvider getLabelProvider() {
	    DelegatingLabelProvider labelProvider = new DelegatingLabelProvider();
	    return new DecoratingLabelProvider(labelProvider, labelProvider);
	}

	protected void refresh() {
		ExecutorsUtil.executeInUI(new Runnable() {
			@Override
			public void run() {
			    viewer.refresh();
				updateEnablement(viewer);
			}
		});
	}

	protected abstract Object getInput();

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
	 * @param viewer The tree viewer. Must not be <code>null</code>.
	 */
	protected void updateEnablement(TreeViewer viewer) {
		Assert.isNotNull(viewer);

	    // Adjust the OK button enablement
	    Button okButton = getButton(IDialogConstants.OK_ID);
	    SWTControlUtil.setEnabled(okButton, isValidSelection());
	}

	protected boolean isValidSelection() {
	    int selCount = viewer.getTree().getSelectionCount();
	    return supportsMultiSelection() ? selCount > 0 : selCount == 1;
	}

	/**
	 * Configure the tree viewer.
	 * <p>
	 * The default implementation does nothing. Subclasses may overwrite this
	 * method to customize the tree viewer before the input gets set.
	 *
	 * @param viewer The tree viewer. Must not be <code>null</code>.
	 */
	protected void configureTreeViewer(TreeViewer viewer) {
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

	/**
	 * Set the selection to be shown when the dialog opens.
	 * @param selection The selection to set.
	 */
	public void setSelection(ISelection selection) {
		this.selection = selection;
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
