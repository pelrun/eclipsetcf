/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.ui.views.handler;

import java.util.Iterator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.te.ui.views.navigator.nodes.NewWizardNode;
import org.eclipse.tcf.te.ui.wizards.newWizard.AbstractNewSingleWizardHandler;
import org.eclipse.ui.handlers.HandlerUtil;


/**
 * New configuration wizard handler implementation.
 */
public class NewWizardNodeHandler extends AbstractNewSingleWizardHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.wizards.newWizard.AbstractNewSingleWizardHandler#getWizardId(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	protected String getWizardId(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Iterator<?> iterator = ((IStructuredSelection)selection).iterator();
			while (iterator.hasNext()) {
				Object element = iterator.next();
				if (element instanceof NewWizardNode) {
					return ((NewWizardNode)element).getWizardId();
				}
			}
		}
		return null;
	}
}
