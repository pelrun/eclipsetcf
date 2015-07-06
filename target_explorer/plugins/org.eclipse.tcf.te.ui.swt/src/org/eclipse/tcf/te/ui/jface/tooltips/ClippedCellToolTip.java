/**
 * ClippedCellToolTip.java
 * Created on Jul 3, 2015
 *
 * Copyright (c) 2015 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.ui.jface.tooltips;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * A tooltip which shows the full content of clipped viewer cell text.
 */
public class ClippedCellToolTip extends DefaultToolTip {

	@SuppressWarnings("unused")
	public static void enableFor(ColumnViewer viewer) {
		if (Platform.OS_WIN32.equals(Platform.getOS()))
			return;
		new ClippedCellToolTip(viewer, NO_RECREATE);
	}

	private final ColumnViewer fViewer;

	/**
	 * Constructor.
	 * @param viewer
	 * @param style
	 */
	protected ClippedCellToolTip(ColumnViewer viewer, int style) {
		super(viewer.getControl(), style, false);
		fViewer = viewer;
	}

	@Override
	protected Object getToolTipArea(Event event) {
		return fViewer.getCell(new Point(event.x, event.y));
	}

	@Override
	protected boolean shouldCreateToolTip(Event event) {
		if (!super.shouldCreateToolTip(event))
			return false;

		ColumnViewer viewer = fViewer;

		Point point = new Point(event.x, event.y);
		ViewerCell cell = viewer.getCell(point);
		if (cell == null)
			return false;

		String text = cell.getText();
		Rectangle cellBounds = cell.getBounds();
		Rectangle textBounds = cell.getTextBounds();
		int maxWidth = textBounds != null ? textBounds.width : cellBounds.width;

		GC gc = new GC(viewer.getControl());
		gc.setFont(cell.getFont());
		int width = gc.textExtent(text).x;
		gc.dispose();

		if (width <= maxWidth) {
			// test against viewer bounds
			int cellMargin = cellBounds.x + width + 4;
			Rectangle viewerBounds = viewer.getControl().getBounds();
			int viewerMargin = viewerBounds.x + viewerBounds.width - 4;
			ScrollBar vsb = null;
			if (viewer instanceof TableViewer)
				vsb = ((TableViewer) viewer).getTable().getVerticalBar();
			else if (viewer instanceof TreeViewer)
				vsb = ((TreeViewer) viewer).getTree().getVerticalBar();
			if (vsb != null && vsb.isVisible())
				viewerMargin -= vsb.getSize().x;
			if (cellMargin <= viewerMargin)
				return false;
		}
		setText(wrapText(text));
		return true;
	}

	@Override
	protected void afterHideToolTip(Event event) {
		super.afterHideToolTip(event);
		if (event != null && event.widget != fViewer.getControl()) {
			fViewer.getControl().setFocus();
		}
	}

	private String wrapText(String tooltipText) {
		StringBuilder buf = new StringBuilder();
		final int maxCol = 80;
		int col = 0;
		int wordIdx = 0;
		int breakIdx = 0;
		int i;
		for (i = 0; i<tooltipText.length(); ++i) {
			char c = tooltipText.charAt(i);
			if (col >= maxCol && c != '\n') {
				if (breakIdx == wordIdx) {
					if (wordIdx > 0) buf.append('\n');
					buf.append(tooltipText.substring(wordIdx, i));
					col = i - wordIdx;
					wordIdx = i;
				} else {
					buf.append(tooltipText.substring(wordIdx, i));
					wordIdx = breakIdx = i;
				}
			}
			switch (c) {
			case ' ':
				buf.append(tooltipText.substring(wordIdx, i));
				buf.append(c);
				++col;
				wordIdx = breakIdx = i+1;
				continue;
			case '\n':
				buf.append(tooltipText.substring(wordIdx, i));
				buf.append(c);
				col = 0;
				wordIdx = breakIdx = i+1;
				continue;
			}
			++col;
		}
		buf.append(tooltipText.substring(wordIdx, i));
		return buf.toString();
	}
}
