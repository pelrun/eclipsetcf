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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.TableViewer;
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

	        TableViewer viewer = getViewer();
	        if (viewer != null) {
	        	Table table = viewer.getTable();
	        	Object layoutData = table.getLayoutData();
	        	if (layoutData instanceof GridData) {
	        		((GridData)layoutData).widthHint = SWT.DEFAULT;
	        	}

	        	TableColumn[] columns = table.getColumns();
	        	for (int i = 0; i < columns.length; i++) {
	        		switch (i) {
	        		case 0:
	        			columns[i].setWidth(37);
						break;
					case 1:
	                case 2:
	                    columns[i].setWidth(10);
	                    break;
	                case 4:
	                    columns[i].setWidth(18);
	                    break;
	                default:
	                    columns[i].setWidth(7);
	                    break;
	        		}
	        	}

	        	TableUtils.adjustTableColumnWidth(viewer);
	        }
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
		performApply(parentEditorPage.getLaunchConfig(parentEditorPage.getPeerModel(parentEditorPage.getEditorInput())));
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
