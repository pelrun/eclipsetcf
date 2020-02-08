/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.internal.tabbed;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ISysMonitor;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.ui.tabbed.BaseTitledSection;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * The property section to display the basic context information of a process.
 */
public class BasicContextSection extends BaseTitledSection {
	// The process context to be displayed.
	/* default */ ISysMonitor.SysMonitorContext context;
	// The text field for the executable file.
	private Text fileText;
	// The text field for the working directory.
	private Text workDirText;
	// The text field for the root directory.
	private Text rootText;
	// The state of the process.
	private Text stateText;
	// The owner of the process.
	private Text userText;
	// The owner group of the process.
	private Text groupText;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#createControls(org.eclipse.swt.widgets.Composite, org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
    public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
	    super.createControls(parent, aTabbedPropertySheetPage);
	    fileText = createWrapTextField(null, Messages.BasicContextSection_File);
		workDirText = createWrapTextField(fileText, Messages.BasicContextSection_WorkDir);
		rootText = createWrapTextField(workDirText, Messages.BasicContextSection_Root);
		stateText = createTextField(rootText, Messages.BasicContextSection_State);
		userText = createTextField(stateText, Messages.BasicContextSection_User);
		groupText = createTextField(userText, Messages.BasicContextSection_Group);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#updateData(org.eclipse.tcf.te.ui.interfaces.IPropertyChangeProvider)
	 */
	@Override
    protected void updateInput(IPeerNodeProvider input) {
        Assert.isTrue(input instanceof IProcessContextNode);
        final IProcessContextNode node = (IProcessContextNode) input;

        Runnable runnable = new Runnable() {
			@Override
			public void run() {
				context = node.getSysMonitorContext();
			}
		};

		Assert.isTrue(!Protocol.isDispatchThread());
		Protocol.invokeAndWait(runnable);
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#refresh()
	 */
	@Override
    public void refresh() {
		SWTControlUtil.setText(fileText, context == null ? "" : (context.getFile() == null ? "" : context.getFile())); //$NON-NLS-1$ //$NON-NLS-2$
		SWTControlUtil.setText(workDirText, context == null ? "" : (context.getCurrentWorkingDirectory() == null ? "" : context.getCurrentWorkingDirectory())); //$NON-NLS-1$ //$NON-NLS-2$
		SWTControlUtil.setText(rootText, context == null ? "" : (context.getRoot() == null ? "" : context.getRoot())); //$NON-NLS-1$ //$NON-NLS-2$
		SWTControlUtil.setText(stateText, context == null ? "" : (context.getState() == null ? "" : context.getState())); //$NON-NLS-1$ //$NON-NLS-2$
		SWTControlUtil.setText(userText, context == null ? "" : (context.getUserName() == null ? "" : context.getUserName())); //$NON-NLS-1$ //$NON-NLS-2$
		SWTControlUtil.setText(groupText, context == null ? "" : (context.getGroupName() == null ? "" : context.getGroupName())); //$NON-NLS-1$ //$NON-NLS-2$
		super.refresh();
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#getText()
	 */
	@Override
	protected String getText() {
		return Messages.BasicContextSection_Title;
	}
}
