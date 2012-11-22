/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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

		final TableViewer tableViewer = (TableViewer)viewer;
		final Table table = tableViewer.getTable();
		table.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				int sumColumnWidth = 0;
				int tableWidth = table.getSize().x - table.getVerticalBar().getSize().x;

				TableColumn[] columns = table.getColumns();

				// Summarize the table column width
				for (TableColumn column : columns) {
					sumColumnWidth += column.getWidth();
				}

				// Calculate the new width for each column
				int sumColumnWidth2 = 0;
				TableColumn maxColumn = null;
				for (TableColumn column : columns) {
					int weight = (column.getWidth() * 100) / sumColumnWidth;
					int newWidth = (weight * tableWidth) / 100;
					sumColumnWidth2 += newWidth;
					column.setWidth(newWidth);
					if (maxColumn == null || maxColumn.getWidth() < column.getWidth()) {
						maxColumn = column;
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