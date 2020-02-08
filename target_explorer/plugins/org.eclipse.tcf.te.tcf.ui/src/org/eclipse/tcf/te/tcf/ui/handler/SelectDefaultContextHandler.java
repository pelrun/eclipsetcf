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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IDefaultContextService;
import org.eclipse.tcf.te.tcf.ui.dialogs.PeerNodeSelectionDialog;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * AbstractHandler
 */
public class SelectDefaultContextHandler extends AbstractHandler {

	/**
     * Constructor.
     */
    public SelectDefaultContextHandler() {
    	super();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

    	PeerNodeSelectionDialog dialog = new PeerNodeSelectionDialog(HandlerUtil.getActiveShell(event)) {
    		@Override
    		protected String getTitle() {
    		    return Messages.SelectDefaultContextHandler_dialog_title;
    		}
    		/* (non-Javadoc)
    		 * @see org.eclipse.tcf.te.tcf.ui.dialogs.PeerNodeSelectionDialog#getDefaultMessage()
    		 */
    		@Override
    		protected String getDefaultMessage() {
    		    return Messages.SelectDefaultContextHandler_dialog_message;
    		}
    		@Override
    		protected Object[] getInput() {
    			IPeerNode defaultPeer = ServiceManager.getInstance().getService(IDefaultContextService.class).getDefaultContext(null);
    		    Object[] peerNodes = super.getInput();
    		    List<Object> input = new ArrayList<Object>();
    		    for (Object object : peerNodes) {
	                if (!object.equals(defaultPeer)) {
	                	input.add(object);
	                }
                }

    		    return input.toArray();
    		}
    	};

    	if (dialog.open() == Window.OK) {
    		ISelection selection = dialog.getSelection();
    		if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).getFirstElement() instanceof IPeerNode) {
    			ServiceManager.getInstance().getService(IDefaultContextService.class).setDefaultContext((IPeerNode)((IStructuredSelection)selection).getFirstElement());
    		}
    	}

	    return null;
    }
}
