/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.events.TriggerCommandEvent;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.navigator.wizards.CommonWizardDescriptor;
import org.eclipse.ui.internal.navigator.wizards.CommonWizardDescriptorManager;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.WizardActionGroup;

/**
 * Connect peer command handler implementation.
 */
@SuppressWarnings("restriction")
public class ConnectPeerCommandHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
    @Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Get the selection from the event
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			// The selection contains only one element as multi element selections are not supported by this handler
			Object element = ((IStructuredSelection)selection).getFirstElement();
			// The element must be of type IPeer
			if (element instanceof IPeer) {
				// Get the list of enabled new wizards
				IWorkbenchPart part = HandlerUtil.getActivePart(event);
				if (part instanceof CommonNavigator) {
					CommonWizardDescriptor[] wizards = CommonWizardDescriptorManager.getInstance().getEnabledCommonWizardDescriptors(element, WizardActionGroup.TYPE_NEW, ((CommonNavigator)part).getNavigatorContentService());
					// If there are more than one wizard, the user must select which wizard
					// to use to create the connection -> open the new connection wizard
					if (wizards.length > 1) {
						TriggerCommandEvent e = new TriggerCommandEvent(element, "org.eclipse.tcf.te.ui.command.newWizards"); //$NON-NLS-1$
						EventManager.getInstance().fireEvent(e);
					}
				}
			}
		}

		return null;
	}

}
