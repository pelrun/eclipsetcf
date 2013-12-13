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

import java.util.HashMap;
import java.util.Map;

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
import org.eclipse.tcf.services.IProcesses;
import org.eclipse.tcf.services.ISysMonitor;
import org.eclipse.tcf.te.runtime.model.interfaces.IModelNode;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IUIService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.processes.core.model.interfaces.IProcessContextNode;
import org.eclipse.tcf.te.tcf.processes.ui.interfaces.IProcessMonitorUIDelegate;
import org.eclipse.tcf.te.tcf.processes.ui.nls.Messages;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * The general information page of a process' properties dialog.
 */
public class GeneralInformationPage extends PropertyPage {

	/* default */ IProcessContextNode node;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
    @Override
	protected Control createContents(Composite parent) {
		IAdaptable element = getElement();
		Assert.isTrue(element instanceof IProcessContextNode);

		node = (IProcessContextNode) element;

		IPeerNode peerNode = (IPeerNode)node.getAdapter(IPeerNode.class);
		IUIService service = peerNode != null ? ServiceManager.getInstance().getService(peerNode, IUIService.class) : null;
		IProcessMonitorUIDelegate delegate = service != null ? service.getDelegate(peerNode, IProcessMonitorUIDelegate.class) : null;

		final Map<String, Object> props = new HashMap<String, Object>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				props.putAll(node.getProcessContext().getProperties());
				props.putAll(node.getSysMonitorContext().getProperties());
				props.put(IModelNode.PROPERTY_TYPE, node.getType().toString());
			}
		};
		Assert.isTrue(!Protocol.isDispatchThread());
		Protocol.invokeAndWait(runnable);

		Composite page = new Composite(parent, SWT.NONE);
		GridLayout gridLayout = new GridLayout(2, false);
		page.setLayout(gridLayout);

		createField(Messages.GeneralInformationPage_Name, props.get(IProcesses.PROP_NAME), page);
		createField(Messages.GeneralInformationPage_Type, props.get(IModelNode.PROPERTY_TYPE), page);
		createField(Messages.GeneralInformationPage_State, props.get(ISysMonitor.PROP_STATE), page);
		createField(Messages.GeneralInformationPage_User, props.get(ISysMonitor.PROP_USERNAME), page);
		createSeparator(page);

		String label = Messages.getStringDelegated(peerNode, "GeneralInformationPage_ProcessID"); //$NON-NLS-1$
		String value = delegate != null ? delegate.getText(node, "PID", (props.get(ISysMonitor.PROP_PID) != null ? props.get(ISysMonitor.PROP_PID).toString() : null)) : null; //$NON-NLS-1$
		createField(label != null ? label : Messages.GeneralInformationPage_ProcessID, value != null ? value : props.get(ISysMonitor.PROP_PID), page);
		label = Messages.getStringDelegated(peerNode, "GeneralInformationPage_ParentPID"); //$NON-NLS-1$
		value = delegate != null ? delegate.getText(node, "PPID", (props.get(ISysMonitor.PROP_PPID) != null ? props.get(ISysMonitor.PROP_PPID).toString() : null)) : null; //$NON-NLS-1$
		createField(label != null ? label : Messages.GeneralInformationPage_ParentPID, props.get(ISysMonitor.PROP_PPID), page);
		label = Messages.getStringDelegated(peerNode, "GeneralInformationPage_InternalPID"); //$NON-NLS-1$
		createField(label != null ? label : Messages.GeneralInformationPage_InternalPID, props.get(IProcesses.PROP_ID), page);
		label = Messages.getStringDelegated(peerNode, "GeneralInformationPage_InternalPPID"); //$NON-NLS-1$
		createField(label != null ? label : Messages.GeneralInformationPage_InternalPPID, props.get(IProcesses.PROP_PARENTID), page);

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
