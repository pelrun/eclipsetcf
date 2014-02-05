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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.events.TriggerCommandEvent;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.services.interfaces.IUIService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.wizards.newWizard.NewWizardRegistry;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.IWizardDescriptor;

/**
 * NewToolbarCommandHandler
 */
public class NewToolbarCommandHandler extends AbstractHandler {

	private String[] wizardIds;

	/**
     * Constructor.
     */
    public NewToolbarCommandHandler() {
    	super();
    }

	private void init() {
		IPeerModel peerModel = ModelManager.getPeerModel();
		IService[] services = ServiceManager.getInstance().getServices(peerModel, IUIService.class, false);
		List<String> ids = new ArrayList<String>();
		for (IService service : services) {
	        if (service instanceof IUIService) {
	        	IDefaultContextToolbarDelegate delegate = ((IUIService)service).getDelegate(peerModel, IDefaultContextToolbarDelegate.class);
	        	if (delegate != null) {
	        		String[] newIds = delegate.getToolbarNewConfigWizardIds(peerModel);
	        		if (newIds != null) {
	        			for (String newId : newIds) {
	        				if (!ids.contains(newId)) ids.add(newId);
                        }
	        		}
	        	}
	        }
        }

		wizardIds = ids.toArray(new String[ids.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		openNewWizard();
	    return null;
	}

    private void openNewWizard() {
    	init();
		if (wizardIds.length == 1) {
			IWizardDescriptor wizardDesc = NewWizardRegistry.getInstance().findWizard(wizardIds[0]);
			if (wizardDesc == null) return;
	    	IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

	    	try {
	    		WizardDialog wd = new WizardDialog(window.getShell(), wizardDesc.createWizard());
	    		wd.setTitle(Messages.NewTargetWizard_windowTitle);
	    		wd.open();
	    	}
	    	catch (Exception e) {
	    	}
		}
		else {
			TriggerCommandEvent event = new TriggerCommandEvent(this, "org.eclipse.tcf.te.ui.command.newWizards"); //$NON-NLS-1$
			EventManager.getInstance().fireEvent(event);
		}
	}
}
