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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.tcf.internal.debug.ui.launch.TCFPathMapTab;
import org.eclipse.tcf.te.tcf.launch.ui.editor.AbstractTcfLaunchTabContainerEditorPage;
import org.eclipse.tcf.te.tcf.launch.ui.nls.Messages;

/**
 * Customized TCF path map launch configuration tab implementation to work better
 * inside an configuration editor tab.
 */
public class PathMapTab extends TCFPathMapTab {
	// Reference to the parent editor page
	private final AbstractTcfLaunchTabContainerEditorPage parentEditorPage;

	/**
     * Constructor
     *
     * @param parentEditorPage The parent editor page. Must not be <code>null</code>.
     */
    public PathMapTab(AbstractTcfLaunchTabContainerEditorPage parentEditorPage) {
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
	 * @see org.eclipse.tcf.internal.debug.ui.launch.TCFPathMapTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
	    super.createControl(parent);

	    TableViewer viewer = getViewer();
	    if (viewer != null) {
	    	TableColumn[] columns = viewer.getTable().getColumns();
	    	for (TableColumn column : columns) {
	    		if (column.getWidth() == 300) column.setWidth(27);
	    		else if (column.getWidth() == 100) column.setWidth(15);

	    		String label = column.getText();
	    		String key = "PathMapEditorPage_column_" + label.toLowerCase(); //$NON-NLS-1$
	    		if (Messages.hasString(key)) column.setText(Messages.getString(key));
	    	}

	    	TableUtils.adjustTableColumnWidth(viewer);
	    }
	}
}
