/*******************************************************************************
 * Copyright (c) 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.handler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;

public class RenameToolbarCommandHandler extends RenameHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.handler.RenameHandler#getSelection(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected ISelection getSelection(ExecutionEvent event) {
		return new StructuredSelection(ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null));
	}
}
