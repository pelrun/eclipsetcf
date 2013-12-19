/**
 * MapLabelProvider.java
 * Created on Sep 14, 2013
 *
 * Copyright (c) 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.tcf.ui.tabbed;

import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * The label provider to provide texts and images of map entries.
 */
public class MapLabelProvider extends LabelProvider implements ITableLabelProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof Entry) {
			Entry<?, ?> entry = (Entry<?, ?>) element;
			if (columnIndex == 0) {
				Object key = entry.getKey();
				return key == null ? "" : key.toString(); //$NON-NLS-1$
			}
			Object object = entry.getValue();
			if (object instanceof List<?>) {
				@SuppressWarnings("unchecked")
                List<Object> list = (List<Object>)object;
				if (columnIndex < list.size()) {
					object = list.get(columnIndex);
				} else {
					object = null;
				}
			}

			return object == null ? "" : object.toString(); //$NON-NLS-1$
		}
		return null;
	}
}