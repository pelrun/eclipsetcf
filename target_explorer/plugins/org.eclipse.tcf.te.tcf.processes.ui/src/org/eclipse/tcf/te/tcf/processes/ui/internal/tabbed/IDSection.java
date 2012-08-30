/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModelProvider;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.ui.tabbed.BaseTitledSection;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * The property section to display the IDs of a process.
 */
public class IDSection extends BaseTitledSection {
	// The system monitor context for the selected process node.
	/* default */ ISysMonitor.SysMonitorContext context;
	// The text field to display the process id.
	private Text pidText;
	// The text field to display the parent process id.
	private Text ppidText;
	// The  text field to display the internal process id.
	private Text ipidText;
	// The text field to display the internal parent process id.
	private Text ippidText;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#createControls(org.eclipse.swt.widgets.Composite, org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
    public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
	    super.createControls(parent, aTabbedPropertySheetPage);
	    pidText = createTextField(null, Messages.IDSection_ProcessID);
		ppidText = createTextField(pidText, Messages.IDSection_ParentID);
		ipidText = createTextField(ppidText, Messages.IDSection_InternalID);
		ippidText = createTextField(ipidText, Messages.IDSection_InternalPPID);
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#updateData(org.eclipse.tcf.te.ui.interfaces.IPropertyChangeProvider)
	 */
	@Override
    protected void updateInput(IPeerModelProvider input) {
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

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.properties.tabbed.AbstractPropertySection#refresh()
	 */
	@Override
    public void refresh() {
		SWTControlUtil.setText(pidText, context != null && context.getPID() >= 0 ? Long.toString(context.getPID()) : ""); //$NON-NLS-1$
		SWTControlUtil.setText(ppidText, context != null && context.getPPID() >= 0 ? Long.toString(context.getPPID()) : ""); //$NON-NLS-1$
		SWTControlUtil.setText(ipidText, context != null && context.getID() != null ? context.getID() : ""); //$NON-NLS-1$
		SWTControlUtil.setText(ippidText, context != null && context.getParentID() != null ? context.getParentID() : ""); //$NON-NLS-1$
		super.refresh();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#getText()
	 */
	@Override
	protected String getText() {
		return Messages.IDSection_Title;
	}
}
