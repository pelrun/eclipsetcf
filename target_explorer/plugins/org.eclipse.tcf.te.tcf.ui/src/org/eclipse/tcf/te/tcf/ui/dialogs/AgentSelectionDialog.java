/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModelProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelLookupService;
import org.eclipse.tcf.te.tcf.locator.model.Model;
import org.eclipse.tcf.te.tcf.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.tcf.te.ui.views.navigator.DelegatingLabelProvider;

/**
 * TCF agent selection dialog implementation.
 */
public class AgentSelectionDialog extends CustomTitleAreaDialog {
	// The list of remote services the agents must provide to be included
	/* default */ final String[] services;

	// The table viewer
	/* default */ TableViewer viewer;

	// Button to filter non-reachable targets
	/* default */ Button showOnlyReachable;

	// The selection. Will be filled in if either "OK" or "Cancel" is pressed
	private ISelection selection;

	/**
	 * Constructor.
	 *
	 * @param services The list of (remote) services the agents must provide to be selectable, or <code>null</code>.
	 */
	public AgentSelectionDialog(String[] services) {
		this(null, services);
	}

	/**
	 * Constructor.
	 *
	 * @param parent The parent shell used to view the dialog, or <code>null</code>.
	 * @param services The list of (remote) services the agents must provide to be selectable, or <code>null</code>.
	 */
	public AgentSelectionDialog(Shell parent, String[] services) {
		super(parent, IContextHelpIds.AGENT_SELECTION_DIALOG);

		this.services = services != null && services.length > 0 ? services : null;
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
		return true;
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

	    // Create the table viewer
	    viewer = new TableViewer(parent, (supportsMultiSelection() ? SWT.MULTI : SWT.SINGLE) | SWT.BORDER);

	    // Configure the table
	    Table table = viewer.getTable();

		@SuppressWarnings("unused")
        TableColumn column = new TableColumn(table, SWT.LEFT);

		TableLayout tableLayout = new TableLayout();
		tableLayout.addColumnData(new ColumnWeightData(100));
		table.setLayout(tableLayout);

	    GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
	    table.setLayoutData(layoutData);

	    viewer.setContentProvider(new ArrayContentProvider());
	    DelegatingLabelProvider labelProvider = new DelegatingLabelProvider() {
	    	/* (non-Javadoc)
	    	 * @see org.eclipse.tcf.te.tcf.ui.navigator.LabelProviderDelegate#decorateImage(org.eclipse.swt.graphics.Image, java.lang.Object)
	    	 */
	    	@Override
	    	public Image decorateImage(Image image, Object element) {
	    	    return image;
	    	}
	    };
	    viewer.setLabelProvider(new DecoratingLabelProvider(labelProvider, labelProvider));

	    // Create the filter buttons area
	    createFilterButtons(parent);

	    // Subclasses may customize the viewer before setting the input
	    configureTableViewer(viewer);

	    // The content to show is static. Do the filtering manually so that
	    // we can disable the OK Button if the dialog would not show any content.
	    final ILocatorModelLookupService service = getModel().getService(ILocatorModelLookupService.class);
	    final List<IPeerModel> nodes = new ArrayList<IPeerModel>();
	    if (service != null) {
	    	nodes.addAll(Arrays.asList(service.lkupPeerModelBySupportedServices(null, services)));
	    	ListIterator<IPeerModel> iterator = nodes.listIterator();
	    	while (iterator.hasNext()) {
	    		IPeerModel node = iterator.next();

	    		String value = node.getPeer().getAttributes().get("ValueAdd"); //$NON-NLS-1$
	    		boolean isValueAdd = value != null && ("1".equals(value.trim()) || Boolean.parseBoolean(value.trim())); //$NON-NLS-1$

	    		if (isValueAdd) iterator.remove();
	    	}
	    }
	    viewer.setInput(nodes.size() > 0 ? nodes.toArray(new IPeerModel[nodes.size()]) : null);
	    viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				if (!viewer.getSelection().isEmpty()) {
					okPressed();
				}
			}
		});

	    // Determine the initial state of the "show only reachable" button. If there are no
	    // reachable target while opening the dialog, this button should be not selected.
	    boolean showOnlyReachableSelected = true;
	    ViewerFilter[] filters = viewer.getFilters();
	    if (filters != null && filters.length > 0) {
	    	TreePath parentPath = new TreePath(new Object[0]);
	    	Object[] result = nodes.toArray();
	    	for (ViewerFilter filter : filters) {
				Object[] filteredResult = filter.filter(viewer, parentPath, result);
				result = filteredResult;
	    	}
	    	showOnlyReachableSelected = result.length > 0;
	    } else {
	    	showOnlyReachableSelected = nodes.size() > 0;
	    }

	    if (showOnlyReachableSelected != SWTControlUtil.getSelection(showOnlyReachable)) {
	    	SWTControlUtil.setSelection(showOnlyReachable, showOnlyReachableSelected);
			viewer.refresh();
			updateEnablement(viewer);
	    }
	}

	/**
	 * Creates a set of filter buttons in between the main dialog area
	 * and the button bar.
	 *
	 * @param parent The parent composite. Must not be <code>null</code>.
	 */
	protected void createFilterButtons(Composite parent) {
		Assert.isNotNull(parent);

		showOnlyReachable = new Button(parent, SWT.CHECK);
		SWTControlUtil.setText(showOnlyReachable, getShowOnlyReachableLabel());
		SWTControlUtil.setSelection(showOnlyReachable, true);
		showOnlyReachable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.refresh();
				updateEnablement(viewer);
			}
		});
	}

	/**
	 * Returns the label of the "Show only reachable targets" filter button.
	 *
	 * @return The label.
	 */
	protected String getShowOnlyReachableLabel() {
		return Messages.AgentSelectionDialog_button_showOnlyReachable;
	}

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

	    // Adjust the OK button enablement
	    Button okButton = getButton(IDialogConstants.OK_ID);
	    SWTControlUtil.setEnabled(okButton, viewer.getTable().getItems().length > 0);
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

		viewer.addFilter(new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parentElement, final Object element) {
				if (element instanceof IPeerModel) {
					final AtomicInteger state = new AtomicInteger(IPeerModelProperties.STATE_UNKNOWN);

					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							state.set(((IPeerModel)element).getIntProperty(IPeerModelProperties.PROP_STATE));
						}
					};

					if (Protocol.isDispatchThread()) runnable.run();
					else Protocol.invokeAndWait(runnable);

					boolean isShowOnlyReachable = SWTControlUtil.getSelection(showOnlyReachable);
					if (isShowOnlyReachable) {
						return state.get() == IPeerModelProperties.STATE_CONNECTED || state.get() == IPeerModelProperties.STATE_REACHABLE || state.get() == IPeerModelProperties.STATE_WAITING_FOR_READY;
					}
				}

				return true;
			}
		});
	}

	/**
	 * Returns the dialog title.
	 *
	 * @return The dialog title.
	 */
	protected String getDialogTitle() {
		return Messages.AgentSelectionDialog_dialogTitle;
	}

	/**
	 * Returns the title.
	 *
	 * @return The title.
	 */
	protected String getTitle() {
		return Messages.AgentSelectionDialog_title;
	}

	/**
	 * Returns the default message.
	 *
	 * @return The default message.
	 */
	protected String getDefaultMessage() {
		return Messages.AgentSelectionDialog_message;
	}

	/**
	 * Returns the locator model instance to use for determining
	 * the dialogs input.
	 *
	 * @return The locator model instance.
	 */
	protected ILocatorModel getModel() {
		return Model.getModel();
	}

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
