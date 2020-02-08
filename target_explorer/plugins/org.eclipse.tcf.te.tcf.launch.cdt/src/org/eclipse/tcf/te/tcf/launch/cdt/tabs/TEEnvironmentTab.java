/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.tabs;

import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Launch config tab to specify environment variables for the remote process.
 */
public class TEEnvironmentTab extends EnvironmentTab {

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);

		// Hide unwanted controls
		hide(appendEnvironment);
		hide(replaceEnvironment);
		hide(envSelectButton);
	}

	@Override
	public String getId() {
		return "org.eclipse.tcf.te.remotecdt.debug.environmentTab"; //$NON-NLS-1$
	}

    private void hide(Control ctrl) {
        if (ctrl != null) {
            ctrl.setVisible(false);
            Object layoutData = ctrl.getLayoutData();
            if (layoutData instanceof GridData) {
                GridData gd = (GridData) layoutData;
                gd.exclude = true;
            }
        }
    }

}
