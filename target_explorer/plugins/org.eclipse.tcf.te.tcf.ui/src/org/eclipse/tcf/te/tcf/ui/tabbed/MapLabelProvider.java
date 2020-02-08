/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
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
			Object key = entry.getKey();
			if (columnIndex == 0) {
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
			if (object instanceof Object[]) {
				Object[] array = (Object[])object;
				String value = ""; //$NON-NLS-1$
				for (Object obj : array) {
					if (value.length() > 0) {
						if ("cmdline".equals(key)) //$NON-NLS-1$
							value += " "; //$NON-NLS-1$
						else
							value += ", "; //$NON-NLS-1$
					}
	                value += obj.toString();
                }
				return value;
			}

			return object == null ? "" : object.toString(); //$NON-NLS-1$
		}
		return null;
	}
}