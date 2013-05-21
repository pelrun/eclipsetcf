/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.editor.tabs;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.tcf.internal.debug.ui.commands.MemoryMapWidget;
import org.eclipse.tcf.internal.debug.ui.launch.TCFMemoryMapTab;
import org.eclipse.tcf.internal.debug.ui.model.TCFNode;
import org.eclipse.tcf.te.tcf.launch.ui.editor.AbstractTcfLaunchTabContainerEditorPage;

/**
 * Customized TCF memory map launch configuration tab implementation to work better
 * inside an configuration editor tab.
 */
public class MemoryMapTab extends TCFMemoryMapTab {
	// Reference to the parent editor page
	private final AbstractTcfLaunchTabContainerEditorPage parentEditorPage;

	/**
	 * Local memory map widget implementation.
	 */
	protected static class MyMemoryMapWidget extends MemoryMapWidget {

		/**
		 * Constructor
		 *
		 * @param composite The parent composite.
		 * @param node The TCF node
		 */
        public MyMemoryMapWidget(Composite composite, TCFNode node) {
	        super(composite, node);
        }

        /* (non-Javadoc)
         * @see org.eclipse.tcf.internal.debug.ui.commands.MemoryMapWidget#configureTableLayout(org.eclipse.swt.widgets.Table, int, int, java.lang.String[])
         */
        @Override
        protected void configureTableLayout(Table table, int widthHint, int heighHint, String[] columnNames) {
        	Assert.isNotNull(table);

            GridData data = new GridData(GridData.FILL_BOTH);
            data.widthHint = SWT.DEFAULT;
            data.heightHint = heighHint;
            table.setLayoutData(data);

            for (int i = 0; i < columnNames.length; i++) {
                final TableColumn column = new TableColumn(table, SWT.LEAD, i);
                column.setMoveable(false);
                column.setText(columnNames[i]);
                switch (i) {
        		case 0:
        			column.setWidth(37);
        			column.setData("widthHint", Integer.valueOf(37)); //$NON-NLS-1$
					break;
				case 1:
                case 2:
        			column.setWidth(10);
        			column.setData("widthHint", Integer.valueOf(10)); //$NON-NLS-1$
                    break;
                case 4:
        			column.setWidth(18);
        			column.setData("widthHint", Integer.valueOf(18)); //$NON-NLS-1$
                    break;
                default:
        			column.setWidth(7);
        			column.setData("widthHint", Integer.valueOf(7)); //$NON-NLS-1$
                    break;
                }
            }

        	TableUtils.adjustTableColumnWidth(table);
        }
	}

	/**
     * Constructor
     *
     * @param parentEditorPage The parent editor page. Must not be <code>null</code>.
     */
    public MemoryMapTab(AbstractTcfLaunchTabContainerEditorPage parentEditorPage) {
    	super();
    	Assert.isNotNull(parentEditorPage);
    	this.parentEditorPage = parentEditorPage;
    }

    /**
     * Returns the parent editor page.
     *
     * @return The parent editor page.
     */
    public final AbstractTcfLaunchTabContainerEditorPage getParentEditorPage() {
    	return parentEditorPage;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tcf.internal.debug.ui.launch.TCFPathMapTab#updateLaunchConfigurationDialog()
     */
	@Override
	protected void updateLaunchConfigurationDialog() {
		super.updateLaunchConfigurationDialog();
		performApply(AbstractTcfLaunchTabContainerEditorPage.getLaunchConfig(parentEditorPage.getPeerModel(parentEditorPage.getEditorInput())));
		parentEditorPage.checkLaunchConfigDirty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.debug.ui.launch.TCFMemoryMapTab#createWidget(org.eclipse.swt.widgets.Composite, org.eclipse.tcf.internal.debug.ui.model.TCFNode)
	 */
	@Override
	protected MemoryMapWidget createWidget(Composite composite, TCFNode node) {
	    return new MyMemoryMapWidget(composite, node);
	}
}
