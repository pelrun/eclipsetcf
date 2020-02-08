/*******************************************************************************
 * Copyright (c) 2013, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Abstract command handler implementation.
 */
public abstract class AbstractCommandHandler extends AbstractHandler {

	/**
	 * Get the Selection for this handler.
	 * @param event The execution event.
	 * @return The current selection.
	 */
	protected IStructuredSelection getSelection(ExecutionEvent event) {
		ISelection sel = HandlerUtil.getCurrentSelection(event);
		if (sel instanceof IStructuredSelection) {
			return (IStructuredSelection)sel;
		}
		return new StructuredSelection();
	}

	/**
	 * Name of dialog settings section.
	 * @return
	 */
	protected String getDialogSettingsSectionName() {
		return getClass().getName();
	}
}
