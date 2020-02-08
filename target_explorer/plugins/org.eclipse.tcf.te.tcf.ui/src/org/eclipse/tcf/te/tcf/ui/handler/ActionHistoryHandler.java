/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.ui.handler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.tcf.te.tcf.ui.dialogs.ActionHistorySelectionDialog;
import org.eclipse.tcf.te.ui.views.handler.OpenEditorHandler;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * ActionHistoryHandler
 */
public class ActionHistoryHandler extends OpenEditorHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.handler.OpenEditorHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ActionHistorySelectionDialog dialog = new ActionHistorySelectionDialog(HandlerUtil.getActiveShell(event), null);
		dialog.open();
	    return null;
	}
}
