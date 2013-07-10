/**
 * NewWizardHandler.java
 * Created on Jul 12, 2012
 *
 * Copyright (c) 2012, 2013 Wind River Systems, Inc.
 *
 * The right to copy, distribute, modify, or otherwise make use
 * of this software may be licensed only pursuant to the terms
 * of an applicable Wind River license agreement.
 */
package org.eclipse.tcf.te.ui.views.handler;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.te.ui.views.navigator.nodes.NewWizardNode;
import org.eclipse.tcf.te.ui.wizards.newWizard.NewWizardRegistry;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.actions.NewWizardShortcutAction;
import org.eclipse.ui.wizards.IWizardDescriptor;


/**
 * New configuration wizard handler implementation.
 */
@SuppressWarnings("restriction")
public class NewWizardHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Get the current selection
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Iterator<?> iterator = ((IStructuredSelection)selection).iterator();
			while (iterator.hasNext()) {
				Object element = iterator.next();
				if (element instanceof NewWizardNode) {
					IWizardDescriptor wizardDesc = NewWizardRegistry.getInstance().findWizard(((NewWizardNode)element).getWizardId());
			        // In Eclipse 4.x, the HandlerUtil.getActiveWorkbenchWindow(event) may return null
			        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
			        if (window == null) window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					new NewWizardShortcutAction(window, wizardDesc).run();
				}
			}
		}

		return null;
	}
}
