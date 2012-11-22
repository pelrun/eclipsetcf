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

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupPanel;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

/**
 * Source lookup launch configuration tab container page implementation.
 */
@SuppressWarnings("restriction")
public class SourceLookupEditorPage extends AbstractTcfLaunchTabContainerEditorPage {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.launch.ui.editor.AbstractLaunchTabContainerEditorPage#createLaunchConfigurationTab()
	 */
	@Override
	protected AbstractLaunchConfigurationTab createLaunchConfigurationTab() {
		return new SourceLookupPanel() {
			@Override
			public void createControl(Composite parent) {
				super.createControl(parent);
				((Composite)getControl()).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			}
			@Override
			protected void updateLaunchConfigurationDialog() {
				super.updateLaunchConfigurationDialog();
				performApply(getLaunchConfig(getPeerModel(getEditorInput())));
				checkLaunchConfigDirty();
			}
			/* (non-Javadoc)
			 * @see org.eclipse.debug.internal.ui.sourcelookup.SourceLookupPanel#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
			 */
			@Override
			public void initializeFrom(ILaunchConfiguration configuration) {
				boolean oldDirty = getEditor().isDirty() || checkLaunchConfigDirty();
				super.initializeFrom(configuration);
				if (!oldDirty && checkLaunchConfigDirty()) {
					extractData();
				}
			}
		};
	}
}
