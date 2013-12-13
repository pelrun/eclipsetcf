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
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.ui.tabbed.BaseTitledSection;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * The property section to display the basic information of a process.
 */
public class BasicInformationSection extends BaseTitledSection {
	// The system monitor context for the selected process node.
	/* default */ ISysMonitor.SysMonitorContext context;
	// The process name
	/* default */ String nodeName;
	// The node type
	/* default */ String nodeType;
	// The text field for the name of the process.
	private Text nameText;
	// The text field for the type of the process.
	private Text typeText;
	// The text field for the state of the process.
	private Text stateText;
	// The text field for the owner of the process.
	private Text userText;

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#createControls(org.eclipse.swt.widgets.Composite, org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
    public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
	    super.createControls(parent, aTabbedPropertySheetPage);
		nameText = createWrapTextField(null, Messages.BasicInformationSection_Name);
		typeText = createTextField(nameText, Messages.BasicInformationSection_Type);
		stateText = createTextField(typeText, Messages.BasicInformationSection_State);
		userText = createTextField(stateText, Messages.BasicInformationSection_User);
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
				nodeName = node.getName();
				nodeType = node.getType().toString();
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
		SWTControlUtil.setText(nameText, nodeName != null ? nodeName: ""); //$NON-NLS-1$
		SWTControlUtil.setText(typeText, nodeType != null ? nodeType : ""); //$NON-NLS-1$
		SWTControlUtil.setText(stateText, context != null && context.getState() != null ? context.getState() : ""); //$NON-NLS-1$
		SWTControlUtil.setText(userText, context != null && context.getUserName() != null ? context.getUserName() : ""); //$NON-NLS-1$
		super.refresh();
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#getText()
	 */
	@Override
	protected String getText() {
		return Messages.BasicInformationSection_Title;
	}
}
