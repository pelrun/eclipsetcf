/**
 * NewWizardNodeHandler.java
 * Created on Jul 12, 2012
 *
 * Copyright (c) 2012, 2014 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
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
