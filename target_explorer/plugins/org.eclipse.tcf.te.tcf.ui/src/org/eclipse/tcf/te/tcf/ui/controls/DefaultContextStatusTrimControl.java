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
import java.util.concurrent.atomic.AtomicReference;

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
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.swt.SWTControlUtil;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

/**
 * Default context status bar trim control implementation.
 */
public class DefaultContextStatusTrimControl extends WorkbenchWindowControlContribution implements IEventListener {
	/* default */ Text text = null;

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
		text.setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		text.setToolTipText(Messages.DefaultContextStatusTrimControl_tooltip);

		String selected = ""; //$NON-NLS-1$

		IDefaultContextService service = ServiceManager.getInstance().getService(IDefaultContextService.class);
		if (service != null) {
			IPeerModel peerModel = service.getDefaultContext(null);
			if (peerModel != null) {
				selected = NLS.bind(Messages.DefaultContextStatusTrimControl_label, peerModel.getName());
			}
		}

		text.setText(selected);

		// Register as listener to the selection service
		EventManager.getInstance().addEventListener(this, ChangeEvent.class, IDefaultContextService.class);

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
		if (event.getSource() instanceof IDefaultContextService) {
			final AtomicReference<String> selected = new AtomicReference<String>(""); //$NON-NLS-1$

			IDefaultContextService service = (IDefaultContextService)event.getSource();
			IPeerModel peerModel = service.getDefaultContext(null);
			if (peerModel != null) {
				selected.set(NLS.bind(Messages.DefaultContextStatusTrimControl_label, peerModel.getName()));
			}

			Runnable runnable = new Runnable() {

				@Override
				public void run() {
					SWTControlUtil.setText(text, selected.get());
					getParent().update(true);
				}
			};

			PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.ContributionItem#isDynamic()
	 */
	@Override
	public boolean isDynamic() {
	    return true;
	}
}
