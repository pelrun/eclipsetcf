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

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ILabelProvider;
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
import org.eclipse.tcf.te.runtime.services.interfaces.IUIService;
import org.eclipse.tcf.te.runtime.services.interfaces.delegates.ILabelProviderDelegate;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
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

		text = new Text(panel, SWT.SINGLE | SWT.READ_ONLY);
		GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, true);
		layoutData.minimumWidth = SWTControlUtil.convertWidthInCharsToPixels(text, 25);
		text.setLayoutData(layoutData);
		text.setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		text.setToolTipText(Messages.DefaultContextStatusTrimControl_tooltip);

		String selected = ""; //$NON-NLS-1$

		IDefaultContextService service = ServiceManager.getInstance().getService(IDefaultContextService.class);
		if (service != null) {
			IPeerNode peerNode = service.getDefaultContext(null);
			if (peerNode != null) {
				IUIService uiService = ServiceManager.getInstance().getService(peerNode, IUIService.class);
				ILabelProviderDelegate delegate = uiService != null ? uiService.getDelegate(peerNode, ILabelProviderDelegate.class) : null;
				if (delegate == null) {
					ILabelProvider provider = (ILabelProvider)Platform.getAdapterManager().getAdapter(peerNode, ILabelProvider.class);
					if (provider instanceof ILabelProviderDelegate) {
						delegate = (ILabelProviderDelegate)provider;
					}
				}
				selected = NLS.bind(Messages.DefaultContextStatusTrimControl_label, delegate != null ? delegate.getText(peerNode) : peerNode.getName());
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
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
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
