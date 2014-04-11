/*******************************************************************************
 * Copyright (c) 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.ui.delegates;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate;

/**
 * AbstractDefaultContextToolbarDelegate
 */
public abstract class AbstractDefaultContextToolbarDelegate implements IDefaultContextToolbarDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate#getToolbarNewConfigWizardIds(java.lang.Object)
	 */
	@Override
	public String[] getToolbarNewConfigWizardIds(Object context) {
		return new String[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate#getToolbarHistoryIds(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String[])
	 */
	@Override
	public String[] getToolbarHistoryIds(IPeerNode peerNode, String[] historyIds) {
	    return historyIds;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate#getLabel(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String, java.lang.String)
	 */
	@Override
	public String getLabel(IPeerNode peerNode, String historyId, String entry) {
	    return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate#getImageDescriptor(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String, java.lang.String)
	 */
	@Override
	public ImageDescriptor getImageDescriptor(IPeerNode peerNode, String historyId, String entry) {
	    return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.interfaces.IDefaultContextToolbarDelegate#execute(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, java.lang.String, java.lang.String, boolean)
	 */
	@Override
	public void execute(IPeerNode peerNode, String historyId, String entry, boolean showDialog) {
	}
}
