/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.cdt.tabs;

import org.eclipse.cdt.debug.ui.ICDebuggerPage;
import org.eclipse.cdt.dsf.gdb.internal.ui.launching.CDebuggerTab;
import org.eclipse.cdt.dsf.gdb.service.SessionType;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Abstract custom debugger tab implementation.
 */
@SuppressWarnings("restriction")
public abstract class TEAbstractDebuggerTab extends CDebuggerTab {
	// Do not change the ID. We want to maintain compatibility
	private final static String DEFAULTS_SET = "org.eclipse.cdt.launch.remote.te.TEDSFDebuggerTab.DEFAULTS_SET"; //$NON-NLS-1$

	/**
	 * Constructor
	 */
	public TEAbstractDebuggerTab(SessionType sessionType, boolean attach) {
		super(sessionType, attach);
	}

	/*
	 * When the launch configuration is created for Run mode, this Debugger tab
	 * is not created because it is not used for Run mode but only for Debug
	 * mode. When we then open the same configuration in Debug mode, the launch
	 * configuration already exists and initializeFrom() is called instead of
	 * setDefaults(). We therefore call setDefaults() ourselves and update the
	 * configuration. If we don't then the user will be required to press Apply
	 * to get the default settings saved. Bug 281970
	 */
	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(DEFAULTS_SET, true);
		super.setDefaults(config);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		try {
			if (config.hasAttribute(DEFAULTS_SET) == false) {
				ILaunchConfigurationWorkingCopy wc;
				wc = config.getWorkingCopy();
				setDefaults(wc);
				wc.doSave();
			}
		} catch (CoreException e) {
		}

		super.initializeFrom(config);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.dsf.gdb.internal.ui.launching.CDebuggerTab#initDebuggerTypes(java.lang.String)
	 */
	@Override
	protected void initDebuggerTypes(String selection) {
		// Workaround to fix NPE in CDebuggerTab#initializeCommonControl
		if ("gdb".equals(selection) || "gdbserver".equals(selection)) { //$NON-NLS-1$ //$NON-NLS-2$
			setDebuggerId(selection);
		}
	    super.initDebuggerTypes(selection);
	}

	@Override
	protected void loadDynamicDebugArea() {
		Composite dynamicTabHolder = getDynamicTabHolder();
		// Dispose of any current child widgets in the tab holder area
		Control[] children = dynamicTabHolder.getChildren();
		for (int i = 0; i < children.length; i++) {
			children[i].dispose();
		}
		setDynamicTab(new TEGdbDebuggerPage());

		ICDebuggerPage debuggerPage = getDynamicTab();
		if (debuggerPage == null) {
			return;
		}
		// Ask the dynamic UI to create its Control
		debuggerPage.setLaunchConfigurationDialog(getLaunchConfigurationDialog());
		debuggerPage.createControl(dynamicTabHolder);
		debuggerPage.getControl().setVisible(true);
		dynamicTabHolder.layout(true);
		contentsChanged();
	}

}
