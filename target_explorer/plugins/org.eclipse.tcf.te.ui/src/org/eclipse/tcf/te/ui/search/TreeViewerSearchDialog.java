/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.search;

import java.util.EventObject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tcf.te.ui.activator.UIPlugin;
import org.eclipse.tcf.te.ui.interfaces.IOptionListener;
import org.eclipse.tcf.te.ui.interfaces.IPreferenceKeys;
import org.eclipse.tcf.te.ui.interfaces.ISearchCallback;
import org.eclipse.tcf.te.ui.interfaces.ISearchable;
import org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog;
import org.eclipse.tcf.te.ui.nls.Messages;

/**
 * The searching dialog used to get the searching input.
 */
public class TreeViewerSearchDialog extends CustomTitleAreaDialog implements ISearchCallback, IOptionListener, IPreferenceKeys {
	// The context help id for this dialog.
	private static final String SEARCH_HELP_ID = "org.eclipse.tcf.te.ui.utils.TreeViewerSearchDialog.help"; //$NON-NLS-1$
	// A new search button's ID.
	private static final int SEARCH_ID = 31;
	private static final int DEFAULT_WIDTH_TRIM = 20;
	private static final int DEFAULT_HEIGHT_TRIM = 160;

	// The searching orientation check box.
	private Button fBtnBackward;
	// The wrap search check box.
	private Button fBtnWrap;
	// The progress monitor part that controls the searching job.
	private ProgressMonitorPart fPmPart;
	// The search engine used to do the searching.
	SearchEngine fSearcher;
	// The tree viewer to be searched.
	TreeViewer fViewer;
	// The searchable of the currently selected element.
	ISearchable fSearchable;
	// The root element
	private Object rootElement;

	/**
	 * Create a searching dialog using the default algorithm and
	 * the default matcher.
	 *
	 * @param viewer The tree viewer to search in.
	 */
	public TreeViewerSearchDialog(TreeViewer viewer, TreePath rootPath) {
		super(viewer.getTree().getShell());
		setShellStyle(SWT.DIALOG_TRIM | SWT.MODELESS);
		setHelpAvailable(true);
		setContextHelpId(SEARCH_HELP_ID);
		fViewer = viewer;
		fSearchable = getSearchable(rootPath);
		Assert.isNotNull(fSearchable);
		fSearcher = new SearchEngine(fViewer, isDepthFirst(), fSearchable, rootPath);
		fSearchable.addOptionListener(this);
		rootElement = rootPath.getLastSegment();
		String text = fSearchable.getSearchMessage(rootElement);
		if (text != null) {
			setDefaultMessage(text, NONE);
		}
	}

	/**
	 * If the algorithm of the search is DFS.
	 *
	 * @return true if it is a DFS search or else false if it is BFS
	 */
	protected boolean isDepthFirst() {
		return UIPlugin.getScopedPreferences().getBoolean(PREF_DEPTH_FIRST_SEARCH);
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case SEARCH_ID:
			searchButtonPressed();
			break;
		case IDialogConstants.CLOSE_ID:
			closePressed();
			break;
		default:
			super.buttonPressed(buttonId);
		}
	}

	/**
	 * Invoked when button "Close" is pressed.
	 */
	protected void closePressed() {
		fSearchable.removeOptionListener(this);
		fSearchable.persistValues(getDialogSettings());
		fSearcher.endSearch();
		setReturnCode(OK);
		close();
	}

	/**
	 * Called when search button is pressed to start a new search.
	 */
	private void searchButtonPressed() {
		getButton(SEARCH_ID).setEnabled(false);
		fSearcher.startSearch(this, fPmPart);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, SEARCH_ID, Messages.TreeViewerSearchDialog_BtnSearchText, true);
		createButton(parent, IDialogConstants.CLOSE_ID, Messages.TreeViewerSearchDialog_BtnCloseText, false);
		fSearchable.restoreValues(getDialogSettings());
		updateButtonState();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.dialogs.ISearchCallback#searchDone(org.eclipse.core.runtime.IStatus, org.eclipse.jface.viewers.TreePath)
	 */
	@Override
	public void searchDone(IStatus status, TreePath path) {
		Button btn = getButton(SEARCH_ID);
		if (btn != null && !btn.isDisposed()) {
			btn.setEnabled(true);
			btn.setFocus();
		}
		if (status.isOK()) {
			if (path == null) {
				if (fSearcher.isWrap()) {
					if (fSearcher.getLastResult() == null) {
						String message = fSearchable.getCustomMessage(rootElement, "TreeViewerSearchDialog_NoSuchNode"); //$NON-NLS-1$
						setMessage(message != null ? message: Messages.TreeViewerSearchDialog_NoSuchNode, IMessageProvider.WARNING);
					}
				}
				else {
					String message = fSearchable.getCustomMessage(rootElement, "TreeViewerSearchDialog_NoMoreNodeFound"); //$NON-NLS-1$
					setMessage(message != null ? message: Messages.TreeViewerSearchDialog_NoMoreNodeFound, IMessageProvider.WARNING);
				}
			}
			else {
				this.setErrorMessage(null);
				setMessage(null);
			}
		}
		else {
			this.setErrorMessage(null);
			setMessage(null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog#createDialogAreaContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createDialogAreaContent(Composite parent) {
	    super.createDialogAreaContent(parent);

	    Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fSearchable.createCommonPart(this, container);
		fSearchable.createAdvancedPart(this, container);

		// Progress monitor part to display or cancel searching process.
		fPmPart = new ProgressMonitorPart(container, null, true);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		fPmPart.setLayoutData(data);
		fPmPart.setVisible(false);

		String title = fSearchable.getSearchTitle(rootElement);
		getShell().setText(title);
		this.setTitle(title);
	}

    /**
     * Create the search direction options in the given container.
     *
     * @param container The parent container. Must not be <code>null</code>.
     */
	public void createSearchDirectionOptions(Composite container) {
		Assert.isNotNull(container);

	    SelectionListener l = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectionChanged(e);
			}
		};

		// Wrap search
		fBtnWrap = new Button(container, SWT.CHECK);
		fBtnWrap.setText(Messages.TreeViewerSearchDialog_BtnWrapText);
		fBtnWrap.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fBtnWrap.addSelectionListener(l);

		if (fSearcher.isDepthFirst()) {
			// Search backward.
			fBtnBackward = new Button(container, SWT.CHECK);
			fBtnBackward.setText(Messages.TreeViewerSearchDialog_BtnBackText);
			fBtnBackward.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			fBtnBackward.addSelectionListener(l);
		}
    }

	/**
	 * Get the searchable of the current selected path.
	 *
	 * @return A searchable object or null if null if cannot be adapted to a searchable.
	 */
	private ISearchable getSearchable(TreePath path) {
		Assert.isNotNull(path);
		Object element = path.getLastSegment();
		if(element != null) {
			if(element instanceof ISearchable) {
				return (ISearchable) element;
			}
			ISearchable searchable = null;
			if(element instanceof IAdaptable) {
				searchable = (ISearchable)((IAdaptable)element).getAdapter(ISearchable.class);
			}
			if(searchable == null) {
				searchable = (ISearchable)Platform.getAdapterManager().getAdapter(element, ISearchable.class);
			}
			return searchable;
		}
		return null;
	}

	/**
	 * Event handler to process a button selection event.
	 *
	 * @param e The selection event.
	 */
	void selectionChanged(SelectionEvent e) {
		Object src = e.getSource();
		if (src == fBtnBackward) {
			fSearcher.endSearch();
			fSearcher.setStartPath(fSearcher.getLastResult());
			fSearcher.setForeward(!fBtnBackward.getSelection());
		}
		else if (src == fBtnWrap) {
			fSearcher.setWrap(fBtnWrap.getSelection());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#getInitialSize()
	 */
	@Override
    protected Point getInitialSize() {
		Point size = fSearchable.getPreferredSize();
		if(size != null) {
			int width = size.x + DEFAULT_WIDTH_TRIM;
			int height = size.y + DEFAULT_HEIGHT_TRIM;
			return new Point(width, height);
		}
	    return super.getInitialSize();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.interfaces.IOptionListener#optionChanged(java.util.EventObject)
	 */
	@Override
    public void optionChanged(EventObject event) {
		fSearcher.resetPath();
		updateButtonState();
    }

	/**
	 * Update the button's action according to
	 */
	protected void updateButtonState() {
		Button button = getButton(SEARCH_ID);
		if (button != null) {
			button.setEnabled(fSearchable.isInputValid());
		}
    }
}
