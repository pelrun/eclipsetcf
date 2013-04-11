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
		performApply(AbstractTcfLaunchTabContainerEditorPage.getLaunchConfig(parentEditorPage.getPeerModel(parentEditorPage.getEditorInput())));
		parentEditorPage.checkLaunchConfigDirty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.debug.ui.launch.TCFPathMapTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
	    super.createControl(parent);

	    TableViewer viewer = getViewer();
	    if (viewer != null) TableUtils.adjustTableColumnWidth(viewer);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.debug.ui.launch.TCFPathMapTab#getColumnText(int)
	 */
	@Override
	protected String getColumnText(int column) {
		String text = super.getColumnText(column);
		String key = "PathMapEditorPage_column_" + text; //$NON-NLS-1$
		if (Messages.hasString(key)) text = Messages.getString(key);
	    return text;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.internal.debug.ui.launch.TCFPathMapTab#getColumnWidth(int)
	 */
	@Override
	protected int getColumnWidth(int column) {
		int width = -1;
		switch (column) {
		case 0:
		case 1:
			width = 27;
			break;
		case 2:
			width = 15;
			break;
		default:
			width = -1;
		}
	    return width != -1 ? width : super.getColumnWidth(column);
	}
}
