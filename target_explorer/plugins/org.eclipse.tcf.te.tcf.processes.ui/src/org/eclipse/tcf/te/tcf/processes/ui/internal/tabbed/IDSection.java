/*******************************************************************************
 * Copyright (c) 2011, 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.internal.tabbed;

import java.math.BigInteger;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ISysMonitor;
import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.ui.interfaces.IProcessMonitorUIDelegate;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.ui.tabbed.BaseTitledSection;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * The property section to display the IDs of a process.
 */
public class IDSection extends BaseTitledSection {
	// The selected process node
	/* default */ IProcessContextNode node;
	// The system monitor context for the selected process node.
	/* default */ ISysMonitor.SysMonitorContext context;
	// The text field to display the process id.
	private CLabel pidLabel;
	private Text pidText;
	// The text field to display the parent process id.
	private CLabel ppidLabel;
	private Text ppidText;
	// The  text field to display the internal process id.
	private CLabel ipidLabel;
	private Text ipidText;
	// The text field to display the internal parent process id.
	private CLabel ippidLabel;
	private Text ippidText;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#createControls(org.eclipse.swt.widgets.Composite, org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
    public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
	    super.createControls(parent, aTabbedPropertySheetPage);

	    pidText = createText(null);
	    pidLabel = createLabel(pidText, Messages.IDSection_ProcessID);

	    ppidText = createText(pidText);
	    ppidLabel = createLabel(ppidText, Messages.IDSection_ParentID);

	    ipidText = createText(ppidText);
	    ipidLabel = createLabel(ipidText, Messages.IDSection_InternalID);

	    ippidText = createText(ipidText);
	    ippidLabel = createLabel(ippidText, Messages.IDSection_InternalPPID);
    }

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#updateData(org.eclipse.tcf.te.ui.interfaces.IPropertyChangeProvider)
	 */
	@Override
    protected void updateInput(IPeerNodeProvider input) {
        Assert.isTrue(input instanceof IProcessContextNode);
        final IProcessContextNode node = (IProcessContextNode) input;
        this.node = node;

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
		IPeerNode peerNode = (IPeerNode)node.getAdapter(IPeerNode.class);
		IProcessMonitorUIDelegate delegate = ServiceUtils.getUIServiceDelegate(peerNode, peerNode, IProcessMonitorUIDelegate.class);

		String label = delegate != null ? delegate.getMessage("IDSection_Title") : null; //$NON-NLS-1$
		if (label != null && section != null && !section.isDisposed()) section.setText(label);

		label = delegate != null ? delegate.getMessage("IDSection_ProcessID") : null; //$NON-NLS-1$
		if (label != null) SWTControlUtil.setText(pidLabel, label);
		label = delegate != null ? delegate.getMessage("IDSection_ParentID") : null; //$NON-NLS-1$
		if (label != null) SWTControlUtil.setText(ppidLabel, label);
		label = delegate != null ? delegate.getMessage("IDSection_InternalID") : null; //$NON-NLS-1$
		if (label != null) SWTControlUtil.setText(ipidLabel, label);
		label = delegate != null ? delegate.getMessage("IDSection_InternalPPID") : null; //$NON-NLS-1$
		if (label != null) SWTControlUtil.setText(ippidLabel, label);

		String value = context != null && context.getPID() >= 0 ? Long.toString(context.getPID()) : ""; //$NON-NLS-1$
		if (value==null || value.length()==0) { // BigInteger conversion
			Object o = context.getProperties().get("PID"); //$NON-NLS-1$
			if (o instanceof BigInteger) value = o.toString();
		}
		String value2 = delegate != null ? delegate.getText(node, "PID", value) : null; //$NON-NLS-1$
		SWTControlUtil.setText(pidText, value2 != null ? value2 : value);

		value = context != null && context.getPPID() >= 0 ? Long.toString(context.getPPID()) : ""; //$NON-NLS-1$
		value2 = delegate != null ? delegate.getText(node, "PPID", value) : null; //$NON-NLS-1$
		SWTControlUtil.setText(ppidText, value2 != null ? value2 : value);

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
