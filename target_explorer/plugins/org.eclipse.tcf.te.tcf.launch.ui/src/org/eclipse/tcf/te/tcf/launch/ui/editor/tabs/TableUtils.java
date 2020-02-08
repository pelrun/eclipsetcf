/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.editor.tabs;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * Provides table utilities.
 */
public final class TableUtils {

	/**
	 * Determines the current visible width of the table and
	 * adjust the column width according to there relative weight.
	 *
	 * @param viewer The viewer or <code>null</code>.
	 */
	public static void adjustTableColumnWidth(Viewer viewer) {
		if (!(viewer instanceof TableViewer)) return;

		TableViewer tableViewer = (TableViewer)viewer;
		adjustTableColumnWidth(tableViewer.getTable());
	}

	/**
	 * Determines the current visible width of the table and
	 * adjust the column width according to there relative weight.
	 *
	 * @param table The table or <code>null</code>.
	 */
	public static void adjustTableColumnWidth(final Table table) {
		if (table == null) return;

		table.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				int sumColumnWidth = 0;
				int tableWidth = table.getSize().x - table.getVerticalBar().getSize().x;

				TableColumn[] columns = table.getColumns();

				// Summarize the table column width
				for (TableColumn column : columns) {
					Object widthHint = column.getData("widthHint"); //$NON-NLS-1$
					sumColumnWidth += widthHint instanceof Integer ? ((Integer)widthHint).intValue() : column.getWidth();
				}

				// Calculate the new width for each column
				int sumColumnWidth2 = 0;
				TableColumn maxColumn = null;
				for (TableColumn column : columns) {
					Object widthHint = column.getData("widthHint"); //$NON-NLS-1$
					int width = widthHint instanceof Integer ? ((Integer)widthHint).intValue() : column.getWidth();
					int weight = (width * 100) / sumColumnWidth;
					int newWidth = (weight * tableWidth) / 100;
					sumColumnWidth2 += newWidth;
					if (column.getResizable()) {
						column.setWidth(newWidth);
						if (maxColumn == null || maxColumn.getWidth() < column.getWidth()) {
							maxColumn = column;
						}
					}
				}

				// If we end up with a slighter larger width of all columns than
				// the table widget is, reduce the size of the largest column
				if (sumColumnWidth2 > tableWidth && maxColumn != null) {
					int delta = sumColumnWidth2 - tableWidth + 2;
					maxColumn.setWidth(maxColumn.getWidth() - delta);
				}

				table.removeControlListener(this);
			}

			@Override
			public void controlMoved(ControlEvent e) {
			}
		});
	}
}
