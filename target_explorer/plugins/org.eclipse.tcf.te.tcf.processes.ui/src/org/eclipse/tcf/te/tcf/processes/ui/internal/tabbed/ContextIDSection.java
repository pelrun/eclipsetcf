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
 * The property section to display the context IDs of a process.
 */
public class ContextIDSection extends BaseTitledSection {
	// The selected process node
	/* default */ IProcessContextNode node;
	// The system monitor context for the selected process node.
	/* default */ ISysMonitor.SysMonitorContext context;
	// The text field to display the id of the process context.
	private Text idText;
	// The text field to display the parent id of the process context.
	private Text parentIdText;
	// The text field to display the process group id.
	private Text pgrpText;
	// The text field to display the process id.
	private CLabel pidLabel;
	private Text pidText;
	// The text field to display the parent process id.
	private CLabel ppidLabel;
	private Text ppidText;
	// The text field to display the process TTY group ID.
	private Text tgidText;
	// The text field to display the tracer process's id.
	private Text tracerPidText;
	// The text field to display the user id of the process.
	private Text uidText;
	// The text field to display the user group id of the process.
	private Text ugidText;

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.tabbed.BaseTitledSection#createControls(org.eclipse.swt.widgets.Composite, org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage)
	 */
	@Override
	public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
		super.createControls(parent, aTabbedPropertySheetPage);
		idText = createTextField(null, Messages.ContextIDSection_ID);
		parentIdText = createTextField(idText, Messages.ContextIDSection_ParentID);
		pgrpText = createTextField(parentIdText, Messages.ContextIDSection_GroupID);


		pidText = createText(pgrpText);
		pidLabel = createLabel(pidText, Messages.ContextIDSection_PID);

		ppidText = createText(pidText);
		ppidLabel = createLabel(ppidText, Messages.ContextIDSection_PPID);

		tgidText = createTextField(ppidText, Messages.ContextIDSection_TTY_GRPID);
		tracerPidText = createTextField(tgidText, Messages.ContextIDSection_TracerPID);
		uidText = createTextField(tracerPidText, Messages.ContextIDSection_UserID);
		ugidText = createTextField(uidText, Messages.ContextIDSection_UserGRPID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.tabbed.BaseTitledSection#updateInput(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProvider)
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

		String label = delegate != null ? delegate.getMessage("ContextIDSection_PID") : null; //$NON-NLS-1$
		if (label != null) SWTControlUtil.setText(pidLabel, label);
		label = delegate != null ? delegate.getMessage("ContextIDSection_PPID") : null; //$NON-NLS-1$
		if (label != null) SWTControlUtil.setText(ppidLabel, label);

		this.idText.setText(context == null ? "" : (context.getID() != null ? context.getID() : "")); //$NON-NLS-1$ //$NON-NLS-2$
		this.parentIdText.setText(context == null ? "" : (context.getParentID() != null ? context.getParentID() : "")); //$NON-NLS-1$ //$NON-NLS-2$
		this.pgrpText.setText(context == null ? "" : (context.getPGRP() >= 0 ? "" + context.getPGRP() : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

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

		this.tgidText.setText(context == null ? "" : (context.getTGID() >= 0 ? "" + context.getTGID() : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.tracerPidText.setText(context == null ? "" : (context.getTracerPID() >= 0 ? "" + context.getTracerPID() : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.uidText.setText(context == null ? "" : (context.getUID() >= 0 ? "" + context.getUID() : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.ugidText.setText(context == null ? "" : (context.getUGID() >= 0 ? "" + context.getUGID() : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		super.refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.tabbed.BaseTitledSection#getText()
	 */
	@Override
	protected String getText() {
		return Messages.ContextIDSection_ContextIDs;
	}

}
