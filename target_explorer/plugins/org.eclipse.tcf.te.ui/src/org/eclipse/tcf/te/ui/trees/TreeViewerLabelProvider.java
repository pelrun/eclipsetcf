/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.trees;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

/**
 * Tree viewer tree control label provider implementation.
 */
public class TreeViewerLabelProvider extends PendingAwareLabelProvider implements ITableLabelProvider, ITableColorProvider, ITableFontProvider {
	// Reference to the parent tree viewer
	private TreeViewer viewer;
	// The parent tree control instance
	private final AbstractTreeControl parentTreeControl;

	/**
	 * Constructor.
	 *
     * @param parentTreeControl The parent tree control instance. Must not be <code>null</code>.
	 * @param viewer The tree viewer or <code>null</code>.
	 */
	public TreeViewerLabelProvider(AbstractTreeControl parentTreeControl, TreeViewer viewer) {
		super();

    	Assert.isNotNull(parentTreeControl);
    	this.parentTreeControl = parentTreeControl;
		this.viewer = viewer;
	}

	/**
	 * Get the specific column's ColumnDescriptor object. <b>NOTE:</b>
	 * <em>The returned descriptor might be null, if the column is the
	 * padding column on linux host.</em>
	 *
	 * @param columnIndex the column index.
	 * @return The ColumnDescriptor object describing the column.
	 */
	private ColumnDescriptor getColumn(int columnIndex) {
		Tree tree = viewer.getTree();
		TreeColumn column = tree.getColumnCount() > columnIndex ? tree.getColumn(columnIndex) : null;
		ColumnDescriptor descriptor = column != null ? (ColumnDescriptor) column.getData() : null;
		return descriptor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		return getColumnText(element, 0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		return getColumnImage(element, 0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (element instanceof Pending) {
			return columnIndex == 0 ? super.getImage(element) : null;
		}
		ColumnDescriptor column = getColumn(columnIndex);
		if (column != null) {
			// Determine if the element is handled by a content contribution
			ContentDescriptor[] descriptors = parentTreeControl.getContentDescriptors();
			if (descriptors != null) {
				for (ContentDescriptor descriptor : descriptors) {
					AbstractContentContribution contribution = descriptor.getContentContribution();
					if (contribution == null) continue;
					if (contribution.isElementHandled(element)) {
						return contribution.getColumnImage(column.getId(), element);
					}
				}
			}
			// Pass on to the main column label provider
			ILabelProvider labelProvider = column.getLabelProvider();
			if (labelProvider != null) {
				return labelProvider.getImage(element);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof Pending) {
			return columnIndex == 0 ? super.getText(element) : ""; //$NON-NLS-1$
		}
		ColumnDescriptor column = getColumn(columnIndex);
		if (column != null) {
			// Determine if the element is handled by a content contribution
			ContentDescriptor[] descriptors = parentTreeControl.getContentDescriptors();
			if (descriptors != null) {
				for (ContentDescriptor descriptor : descriptors) {
					AbstractContentContribution contribution = descriptor.getContentContribution();
					if (contribution == null) continue;
					if (contribution.isElementHandled(element)) {
						return contribution.getColumnText(column.getId(), element);
					}
				}
			}
			// Pass on to the main column label provider
			ILabelProvider labelProvider = column.getLabelProvider();
			if (labelProvider != null) {
				return labelProvider.getText(element);
			}
		}
		return ""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableFontProvider#getFont(java.lang.Object, int)
	 */
	@Override
	public Font getFont(Object element, int columnIndex) {
		if (!(element instanceof Pending)) {
			ColumnDescriptor column = getColumn(columnIndex);
			if (column != null) {
				// Determine if the element is handled by a content contribution
				ContentDescriptor[] descriptors = parentTreeControl.getContentDescriptors();
				if (descriptors != null) {
					for (ContentDescriptor descriptor : descriptors) {
						AbstractContentContribution contribution = descriptor.getContentContribution();
						if (contribution == null) continue;
						if (contribution.isElementHandled(element) && contribution instanceof IFontProvider) {
							return ((IFontProvider)contribution).getFont(element);
						}
					}
				}
				// Pass on to the main column label provider
				ILabelProvider labelProvider = column.getLabelProvider();
				if (labelProvider instanceof IFontProvider) {
					return ((IFontProvider)labelProvider).getFont(element);
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableColorProvider#getForeground(java.lang.Object, int)
	 */
	@Override
	public Color getForeground(Object element, int columnIndex) {
		if (!(element instanceof Pending)) {
			ColumnDescriptor column = getColumn(columnIndex);
			if (column != null) {
				// Determine if the element is handled by a content contribution
				ContentDescriptor[] descriptors = parentTreeControl.getContentDescriptors();
				if (descriptors != null) {
					for (ContentDescriptor descriptor : descriptors) {
						AbstractContentContribution contribution = descriptor.getContentContribution();
						if (contribution == null) continue;
						if (contribution.isElementHandled(element) && contribution instanceof IColorProvider) {
							return ((IColorProvider)contribution).getForeground(element);
						}
					}
				}
				// Pass on to the main column label provider
				ILabelProvider labelProvider = column.getLabelProvider();
				if (labelProvider instanceof IColorProvider) {
					return ((IColorProvider)labelProvider).getForeground(element);
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableColorProvider#getBackground(java.lang.Object, int)
	 */
	@Override
	public Color getBackground(Object element, int columnIndex) {
		if (!(element instanceof Pending)) {
			ColumnDescriptor column = getColumn(columnIndex);
			if (column != null) {
				// Determine if the element is handled by a content contribution
				ContentDescriptor[] descriptors = parentTreeControl.getContentDescriptors();
				if (descriptors != null) {
					for (ContentDescriptor descriptor : descriptors) {
						AbstractContentContribution contribution = descriptor.getContentContribution();
						if (contribution == null) continue;
						if (contribution.isElementHandled(element) && contribution instanceof IColorProvider) {
							return ((IColorProvider)contribution).getBackground(element);
						}
					}
				}
				// Pass on to the main column label provider
				ILabelProvider labelProvider = column.getLabelProvider();
				if (labelProvider instanceof IColorProvider) {
					return ((IColorProvider)labelProvider).getBackground(element);
				}
			}
		}
		return null;
	}
}
