/*******************************************************************************
 * Copyright (c) 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerType;
import org.eclipse.tcf.te.tcf.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.ui.internal.ImageConsts;
import org.eclipse.tcf.te.tcf.ui.navigator.filter.GenericFilter;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;

/**
 * Generic target connections toolbar pull down menu action implementation.
 */
public class ToolbarAction extends AbstractPeerTypeToolbarAction {

	public final static String WIZARD_ID = "org.eclipse.tcf.te.tcf.ui.wizards.NewTargetWizard"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.actions.AbstractPeerTypeToolbarAction#getNewWizardId()
	 */
	@Override
    protected String getNewWizardId() {
		return WIZARD_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.actions.AbstractPeerTypeToolbarAction#getPeerTypeId()
	 */
	@Override
    protected String getPeerTypeId() {
		return IPeerType.TYPE_GENERIC;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.actions.AbstractPeerTypeToolbarAction#getSelectExistingActionLabel()
	 */
	@Override
    protected String getSelectExistingActionLabel() {
		return Messages.ToolbarAction_selectPeer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.actions.AbstractPeerTypeToolbarAction#getSelectExistingDialogTitle()
	 */
	@Override
	protected String getSelectExistingDialogTitle() {
	    return Messages.ToolbarAction_selectionDialog_title;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.actions.AbstractPeerTypeToolbarAction#getSelectExistingDialogDescription()
	 */
	@Override
	protected String getSelectExistingDialogDescription() {
	    return Messages.ToolbarAction_selectionDialog_description;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.actions.AbstractPeerTypeToolbarAction#getSelectExistingDialogViewerFilter()
	 */
	@Override
	protected ViewerFilter getSelectExistingDialogViewerFilter() {
	    return new GenericFilter();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.actions.AbstractPeerTypeToolbarAction#getNewActionLabel()
	 */
	@Override
    protected String getNewActionLabel() {
		return Messages.ContentProviderDelegate_newNode;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.actions.AbstractPeerTypeToolbarAction#getNewActionImageDescriptor()
	 */
	@Override
	protected ImageDescriptor getNewActionImageDescriptor() {
	    return UIPlugin.getImageDescriptor(ImageConsts.NEW_PEER);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.actions.AbstractPeerTypeToolbarAction#getSelectExistingActionImageDescriptor()
	 */
	@Override
	protected ImageDescriptor getSelectExistingActionImageDescriptor() {
	    return UIPlugin.getImageDescriptor(ImageConsts.PEER);
	}
}
