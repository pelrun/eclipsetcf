/*******************************************************************************
 * Copyright (c) 2015 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.internal.handlers;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Handler for adding a file or folder to the favorites
 */
public class ToggleRevealOnConnectContributionItem extends ActionContributionItem {

    private static class ToggleAction extends Action {
        public ToggleAction() {
        	super(Messages.ToggleRevealOnConnectContributionItem_text, IAction.AS_CHECK_BOX);
        }

        @Override
        public void run() {
			boolean value = isChecked();
        	IStructuredSelection selection = getSelection();
        	if (selection != null) {
        		for (Object o : selection.toList()) {
        			if (o instanceof IFSTreeNode) {
						((IFSTreeNode) o).setRevealOnConnect(value);
        			}
				}
        	}
        }

    }

	public ToggleRevealOnConnectContributionItem() {
	    super(new ToggleAction());
    }

	@Override
	public void fill(Menu parent, int index) {
		updateAction();
	    super.fill(parent, index);
	}

	protected static IStructuredSelection getSelection() {
		IWorkbenchWindow ww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (ww == null)
			return null;

		ISelection selection = ww.getSelectionService().getSelection();
		if (selection instanceof IStructuredSelection && !selection.isEmpty())
			return (IStructuredSelection) selection;

		return null;
    }

	private void updateAction() {
		boolean enabled = false;
		int on = 0;

		IStructuredSelection selection = getSelection();
		if (selection != null) {
			for (Object o : selection.toList()) {
				if (!(o instanceof IFSTreeNode)) {
					enabled = false;
					break;
				}
				IFSTreeNode node = (IFSTreeNode) o;
				if (node.isFileSystem()) {
					enabled = false;
					break;
				}
				on += node.isRevealOnConnect() ? 1 : -1;
				enabled = true;
			}
		}
		IAction action = getAction();
		action.setEnabled(enabled);
		action.setChecked(on > 0);
    }
}
