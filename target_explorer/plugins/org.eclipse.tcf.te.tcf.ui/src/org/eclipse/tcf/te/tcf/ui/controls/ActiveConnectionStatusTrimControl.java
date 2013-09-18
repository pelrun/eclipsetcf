/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.controls;

import java.util.EventObject;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tcf.te.runtime.events.ChangeEvent;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.events.IEventListener;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ISelectionService;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

/**
 * Default selection status bar trim control implementation.
 */
public class ActiveConnectionStatusTrimControl extends WorkbenchWindowControlContribution implements IEventListener {
	private Text text = null;

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ControlContribution#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createControl(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0; layout.marginWidth = 0;
		panel.setLayout(layout);

		text = new Text(panel, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		String selected = ""; //$NON-NLS-1$

		ISelectionService service = ServiceManager.getInstance().getService(ISelectionService.class);
		if (service != null) {
			IPeerModel peerModel = service.getDefaultSelection(null);
			if (peerModel != null) {
				selected = NLS.bind(Messages.ActiveConnectionStatusTrimControl_label, peerModel.getName());
			}
		}

		text.setText(selected);

		// Register as listener to the selection service
		EventManager.getInstance().addEventListener(this, ChangeEvent.class, ISelectionService.class);

		return panel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ContributionItem#dispose()
	 */
	@Override
	public void dispose() {
		// Remove ourself as listener
		EventManager.getInstance().removeEventListener(this);

	    super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.runtime.interfaces.events.IEventListener#eventFired(java.util.EventObject)
	 */
	@Override
	public void eventFired(EventObject event) {
		if (event.getSource() instanceof ISelectionService) {
			String selected = ""; //$NON-NLS-1$

			ISelectionService service = (ISelectionService)event.getSource();
			IPeerModel peerModel = service.getDefaultSelection(null);
			if (peerModel != null) {
				selected = NLS.bind(Messages.ActiveConnectionStatusTrimControl_label, peerModel.getName());
			}

			SWTControlUtil.setText(text, selected);
		}
	}
}
