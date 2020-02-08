/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * The base handler to create a new file/folder node in the file system of Target Explorer.
 */
public abstract class NewNodeHandler extends AbstractHandler {

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
        // In Eclipse 4.x, the HandlerUtil.getActiveWorkbenchWindow(event) may return null
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        if (window == null) window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchWizard wizard;
		wizard = createWizard();
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		if (selection instanceof IStructuredSelection) {
			wizard.init(PlatformUI.getWorkbench(), (IStructuredSelection) selection);
		}
		else {
			wizard.init(PlatformUI.getWorkbench(), StructuredSelection.EMPTY);
		}
		Shell parent = window != null ? window.getShell() : null;
		WizardDialog dialog = new WizardDialog(parent, wizard);
		dialog.create();
		dialog.open();
		return null;
	}

	/**
	 * Create a "New" wizard to for creating a file/folder.
	 *
	 * @return the wizard to be used for creating a file/folder.
	 */
	protected abstract IWorkbenchWizard createWizard();
}
