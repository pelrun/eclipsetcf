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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.interfaces.IService;
import org.eclipse.tcf.te.runtime.services.interfaces.IUIService;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.wizards.newWizard.AbstractNewSingleWizardHandler;
import org.eclipse.tcf.te.ui.wizards.newWizard.NewWizard;

/**
 * AbstractNewSingleWizardHandler
 */
public class NewToolbarWizardHandler extends AbstractNewSingleWizardHandler {

	/**
     * Constructor.
     */
    public NewToolbarWizardHandler() {
    	super();
    }


    /* (non-Javadoc)
     * @see org.eclipse.tcf.te.ui.wizards.newWizard.NewWizardCommandHandler#createWizard()
     */
    @Override
    protected IWizard createWizard() {
    	NewWizard wizard = new NewWizard("org.eclipse.tcf.te.tcf.ui.newWizards.category.configurations"); //$NON-NLS-1$
    	wizard.setWindowTitle(Messages.NewTargetWizardPage_title);
        return wizard;
    }

	@Override
    protected String getWizardId(ExecutionEvent event) {
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

		return ids.size() == 1 ? ids.get(0) : null;
	}
}
