/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.attach;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.tcf.te.tcf.launch.core.interfaces.ILaunchTypes;
import org.eclipse.tcf.te.tcf.launch.ui.nls.Messages;
import org.eclipse.ui.PlatformUI;

/**
 * Remote application main launch tab implementation.
 */
public class LaunchConfigurationMainTab extends AbstractLaunchConfigurationTab {

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
	 */
	@Override
	public Image getImage() {
		return DebugUITools.getImage(ILaunchTypes.ATTACH);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
    @Override
    public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		setControl(mainComposite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), getHelpContextId());

		GridLayout layout = new GridLayout();
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		mainComposite.setLayout(layout);
		mainComposite.setLayoutData(gd);
		mainComposite.setFont(parent.getFont());


    	Label info = new Label(mainComposite, SWT.NONE);
    	info.setText(Messages.AttachMainTab_label);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		info.setLayoutData(gd);
    }

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    }

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
    }

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    }

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
    @Override
    public String getName() {
	    return Messages.AttachMainTab_title;
    }
}
