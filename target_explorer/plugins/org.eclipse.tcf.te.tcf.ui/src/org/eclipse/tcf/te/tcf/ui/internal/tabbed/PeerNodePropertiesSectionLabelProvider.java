/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.internal.tabbed;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tcf.te.ui.tables.properties.NodePropertiesTableTableNode;


/**
 * Peer properties general section table label provider implementation.
 */
public class PeerNodePropertiesSectionLabelProvider extends LabelProvider implements ITableLabelProvider {
	// Reference to the parent table viewer
	private final TableViewer parentViewer;

	/**
	 * Constructor.
	 *
	 * @param viewer The table viewer or <code>null</code>.
	 */
	public PeerNodePropertiesSectionLabelProvider(TableViewer viewer) {
		super();
		parentViewer = viewer;
	}

	/**
	 * Returns the parent table viewer instance.
	 *
	 * @return The parent table viewer or <code>null</code>.
	 */
	protected final TableViewer getParentViewer() {
		return parentViewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.vtl.ui.datasource.controls.tables.TableLabelProvider#getColumnText(org.eclipse.tcf.te.tcf.core.runtime.model.interfaces.IModelNode, int)
	 */
	@Override
	public String getColumnText(Object element, int columnIndex) {
		Assert.isNotNull(element);

		String label = null;

		if (element instanceof NodePropertiesTableTableNode) {
			switch (columnIndex) {
				case 0:
					label = ((NodePropertiesTableTableNode)element).name;
					break;
				case 1:
					label = ((NodePropertiesTableTableNode)element).value;
					break;
			}
		}

		return label;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}
}
