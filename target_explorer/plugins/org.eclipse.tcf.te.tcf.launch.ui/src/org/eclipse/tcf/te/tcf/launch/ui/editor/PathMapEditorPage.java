/*******************************************************************************
 * Copyright (c) 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.editor;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.tcf.internal.debug.ui.launch.TCFPathMapTab;
import org.eclipse.tcf.te.tcf.launch.ui.nls.Messages;

/**
 * TCF path map launch configuration tab container page implementation.
 */
public class PathMapEditorPage extends AbstractTcfLaunchTabContainerEditorPage {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.editor.AbstractLaunchTabContainerEditorPage#createLaunchConfigurationTab()
	 */
	@Override
	protected AbstractLaunchConfigurationTab createLaunchConfigurationTab() {
		return new TCFPathMapTab() {
			@Override
			protected void updateLaunchConfigurationDialog() {
				super.updateLaunchConfigurationDialog();
				performApply(getLaunchConfig(getPeerModel(getEditorInput())));
				checkLaunchConfigDirty();
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
			    		String label = column.getText();
			    		String key = "PathMapEditorPage_column_" + label.toLowerCase(); //$NON-NLS-1$
			    		if (Messages.hasString(key)) column.setText(Messages.getString(key));
			    	}
			    }
			}
		};
	}
}
