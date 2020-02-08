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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorNode;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.ui.wizards.newWizard.NewWizard;
import org.eclipse.tcf.te.ui.wizards.newWizard.NewWizardRegistry;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.actions.NewWizardShortcutAction;
import org.eclipse.ui.internal.navigator.wizards.CommonWizardDescriptor;
import org.eclipse.ui.internal.navigator.wizards.CommonWizardDescriptorManager;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.WizardActionGroup;
import org.eclipse.ui.wizards.IWizardDescriptor;

/**
 * Connect peer command handler implementation.
 */
@SuppressWarnings("restriction")
public class ConnectLocatorNodeCommandHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
    @Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		if (window == null) window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		Assert.isNotNull(window);

		// Get the selection from the event
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			// The selection contains only one element as multi element selections are not supported by this handler
			Object element = ((IStructuredSelection)selection).getFirstElement();
			// The element must be of type IPeer
			if (element instanceof ILocatorNode) {
		    	System.setProperty("NewWizard_" + IPeerProperties.PROP_AUTO_CONNECT, Boolean.TRUE.toString()); //$NON-NLS-1$

				// Get the list of enabled new wizards
				IWorkbenchPart part = HandlerUtil.getActivePart(event);
				if (part instanceof CommonNavigator) {
					CommonWizardDescriptor[] wizards = CommonWizardDescriptorManager.getInstance().getEnabledCommonWizardDescriptors(element, WizardActionGroup.TYPE_NEW, ((CommonNavigator)part).getNavigatorContentService());
					// If there are more than one wizard, the user must select which wizard
					// to use to create the connection -> open the new connection wizard
					if (wizards.length > 1) {
				    	NewWizard wizard = new NewWizard("org.eclipse.tcf.te.tcf.ui.newWizards.category.configurations"); //$NON-NLS-1$
				    	wizard.setWindowTitle(Messages.NewTargetWizardPage_title);
				    	wizard.init(window.getWorkbench(), (IStructuredSelection)selection);

				    	WizardDialog dialog = new WizardDialog(HandlerUtil.getActiveShell(event), wizard);
						dialog.create();
						dialog.getShell().setSize(Math.max(400, dialog.getShell().getSize().x), 500);
						window.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IContextHelpIds.NEW_TARGET_WIZARD);
						dialog.open();
					} else if (wizards.length == 1) {
						IWizardDescriptor wizardDesc = NewWizardRegistry.getInstance().findWizard(wizards[0].getWizardId());
						new NewWizardShortcutAction(window, wizardDesc).run();
					}
				}

				System.clearProperty("NewWizard_" + IPeerProperties.PROP_AUTO_CONNECT); //$NON-NLS-1$
			}
		}

		return null;
	}

}
