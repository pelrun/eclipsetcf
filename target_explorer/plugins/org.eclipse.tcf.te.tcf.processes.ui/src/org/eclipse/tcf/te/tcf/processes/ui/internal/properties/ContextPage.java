/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.internal.properties;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.services.ISysMonitor;
import org.eclipse.tcf.services.ISysMonitor.SysMonitorContext;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IUIService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.ui.interfaces.IProcessMonitorUIDelegate;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * The property page to display the context IDs of a process.
 */
public class ContextPage extends PropertyPage {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
    @Override
	protected Control createContents(Composite parent) {
		IAdaptable element = getElement();
		Assert.isTrue(element instanceof IProcessContextNode);

		final IProcessContextNode node = (IProcessContextNode) element;
		Composite page = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(2, false);
		page.setLayout(gridLayout);

		final AtomicReference<ISysMonitor.SysMonitorContext> ctx = new AtomicReference<ISysMonitor.SysMonitorContext>();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				ctx.set(node.getSysMonitorContext());
			}
		};
		Assert.isTrue(!Protocol.isDispatchThread());
		Protocol.invokeAndWait(runnable);

		IPeerNode peerNode = (IPeerNode)node.getAdapter(IPeerNode.class);
		IUIService service = peerNode != null ? ServiceManager.getInstance().getService(peerNode, IUIService.class) : null;
		IProcessMonitorUIDelegate delegate = service != null ? service.getDelegate(peerNode, IProcessMonitorUIDelegate.class) : null;

		SysMonitorContext context = ctx.get();
		createField(Messages.ContextPage_File, context == null ? null : context.getFile(), page);
		createField(Messages.ContextPage_WorkHome, context == null ? null : context.getCurrentWorkingDirectory(), page);
		createField(Messages.ContextPage_Root, context == null ? null : context.getRoot(), page);
		createField(Messages.ContextPage_State, context == null ? null : context.getState(), page);
		createField(Messages.ContextPage_Group, context == null ? null : context.getGroupName(), page);
		createSeparator(page);
		createField(Messages.ContextPage_ID, context == null ? null : context.getID(), page);
		createField(Messages.ContextPage_ParentID, context == null ? null : context.getParentID(), page);
		createField(Messages.ContextPage_GroupID, context == null || context.getPGRP() < 0 ? null : Long.valueOf(context.getPGRP()), page);
		String label = Messages.getStringDelegated(peerNode, "ContextPage_PID"); //$NON-NLS-1$
		Long v = context == null || context.getPID() < 0 ? null : Long.valueOf(context.getPID());
		String value = delegate != null && v != null ? delegate.getText(node, "PID", v.toString()) : null; //$NON-NLS-1$
		createField(label != null ? label : Messages.ContextPage_PID, value != null ? value : v, page);
		label = Messages.getStringDelegated(peerNode, "ContextPage_PPID"); //$NON-NLS-1$
		v = context == null || context.getPPID() < 0 ? null : Long.valueOf(context.getPPID());
		value = delegate != null && v != null ? delegate.getText(node, "PPID", v.toString()) : null; //$NON-NLS-1$
		createField(label != null ? label : Messages.ContextPage_PPID, value != null ? value : v, page);
		createField(Messages.ContextPage_TTYGRPID, context == null || context.getTGID() < 0 ? null : Long.valueOf(context.getTGID()), page);
		createField(Messages.ContextPage_TracerPID, context == null || context.getTracerPID() < 0 ? null : Long.valueOf(context.getTracerPID()), page);
		createField(Messages.ContextPage_UserID, context == null || context.getUID() < 0 ? null : Long.valueOf(context.getUID()), page);
		createField(Messages.ContextPage_UserGRPID, context == null || context.getUGID() < 0 ? null : Long.valueOf(context.getUGID()), page);
		createSeparator(page);
		createField(Messages.ContextPage_Virtual, context == null || context.getVSize() < 0 ? null : Long.valueOf(context.getVSize()), page);
		createField(Messages.ContextPage_Pages, context == null || context.getPSize() < 0 ? null : Long.valueOf(context.getPSize()), page);
		createField(Messages.ContextPage_Resident, context == null || context.getRSS() < 0 ? null : Long.valueOf(context.getRSS()), page);

		return page;
	}
	/**
	 * Create a horizontal separator between field sections.
	 *
	 * @param parent
	 *            The parent composite of the separator.
	 */
	protected void createSeparator(Composite parent) {
		Label label = new Label(parent, SWT.SEPARATOR | SWT.SHADOW_ETCHED_IN | SWT.HORIZONTAL);
		GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		data.horizontalSpan = 2;
		label.setLayoutData(data);
	}
	/**
	 * Create a field displaying the a specific value with a specific label.
	 *
	 * @param text
	 *            The label text for the field.
	 * @param value
	 *            The value to be displayed.
	 * @param parent
	 *            The parent composite of the field.
	 */
	protected void createField(String text, Object value, Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalAlignment = SWT.LEFT;
		data.verticalAlignment = SWT.TOP;
		label.setLayoutData(data);
		Text txt = new Text(parent, SWT.WRAP | SWT.READ_ONLY);
		data = new GridData();
		data.verticalAlignment = SWT.TOP;
		data.widthHint = 300;
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = GridData.FILL;
		txt.setLayoutData(data);
		txt.setBackground(txt.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		txt.setText(value == null ? "" : value.toString()); //$NON-NLS-1$
	}
}
