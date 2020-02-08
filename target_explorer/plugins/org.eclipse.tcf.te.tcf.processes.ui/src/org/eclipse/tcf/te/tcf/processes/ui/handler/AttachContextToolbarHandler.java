/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.processes.ui.handler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.processes.ui.internal.dialogs.AttachContextSelectionDialog;
import org.eclipse.tcf.te.ui.interfaces.IDataExchangeDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Stepper command toolbar handler vor attach.
 */
public class AttachContextToolbarHandler extends AttachContextHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.handler.AbstractCommandHandler#getSelection(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected IStructuredSelection getSelection(ExecutionEvent event) {
		return new StructuredSelection(ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.handler.AbstractContextStepperCommandHandler#getDialog(org.eclipse.core.commands.ExecutionEvent, org.eclipse.tcf.te.runtime.interfaces.properties.IPropertiesContainer)
	 */
	@Override
	protected IDataExchangeDialog getDialog(ExecutionEvent event, IPropertiesContainer data) {
		return new AttachContextSelectionDialog(HandlerUtil.getActiveShell(event), null);
	}
}
