/**
 * AbstractMapPropertiesSection.java
 * Created on Sep 14, 2013
 *
 * Copyright (c) 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.ui.tabbed;

import java.util.Map;

import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertyConstants;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * The property section to display properties in a table.
 */
public abstract class AbstractMapPropertiesSection extends BaseTitledSection {
	/* default */ Map<String,Object> properties;

	// The table control to display the properties.
	protected TableViewer viewer;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#createControls(org.eclipse.swt.widgets.Composite, org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);
		Table table = getWidgetFactory().createTable(composite, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		FormData data = new FormData();
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.top = new FormAttachment(0, ITabbedPropertyConstants.VSPACE);
		data.bottom = new FormAttachment(100, -ITabbedPropertyConstants.VSPACE);
		table.setLayoutData(data);
		TableColumn column = new TableColumn(table, SWT.LEFT);
		column.setText(Messages.AbstractMapPropertiesSection_name_label);
		column = new TableColumn(table, SWT.LEFT);
		column.setText(Messages.AbstractMapPropertiesSection_value_label);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableLayout tableLayout = new TableLayout();
		tableLayout.addColumnData(new ColumnWeightData(40, 100, true));
		tableLayout.addColumnData(new ColumnWeightData(60, 150, true));
		table.setLayout(tableLayout);

		viewer = new TableViewer(table);
		viewer.setContentProvider(getContentProvider(viewer));
		viewer.setLabelProvider(getLabelProvider(viewer));
	}

	protected IContentProvider getContentProvider(TableViewer viewer) {
		return new MapContentProvider();
	}

	protected ILabelProvider getLabelProvider(TableViewer viewer) {
		return new MapLabelProvider();
	}

	protected abstract Object getViewerInput();

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#refresh()
	 */
	@Override
	public void refresh() {
		if (viewer != null && !viewer.getTable().isDisposed()) {
			viewer.setInput(getViewerInput());
			viewer.refresh();
		}

		super.refresh();
	}
}
