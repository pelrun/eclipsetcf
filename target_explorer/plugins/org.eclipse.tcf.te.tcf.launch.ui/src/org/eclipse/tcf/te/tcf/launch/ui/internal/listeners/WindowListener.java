/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.launch.ui.internal.listeners;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.tcf.te.ui.views.listeners.AbstractWindowListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Window listener implementation.
 */
public class WindowListener extends AbstractWindowListener {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.listeners.AbstractWindowListener#createPartListener()
	 */
	@Override
	protected IPartListener2 createPartListener() {
	    return new PartListener();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.listeners.AbstractWindowListener#windowOpened(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
	public void windowOpened(IWorkbenchWindow window) {
	    super.windowOpened(window);

	    // If the debug view is already opened in the workbench window,
	    // make sure the part listener does know about it
	    if (window != null && window.getActivePage() != null && partListener != null) {
	    	IViewReference ref = window.getActivePage().findViewReference(IDebugUIConstants.ID_DEBUG_VIEW);
	    	IViewPart part = ref != null ? ref.getView(false) : null;
	    	if (part != null) partListener.partOpened(ref);
	    }
	}
}
