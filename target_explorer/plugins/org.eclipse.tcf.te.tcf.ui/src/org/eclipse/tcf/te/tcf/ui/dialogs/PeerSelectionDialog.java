/*******************************************************************************
 * Copyright (c) 2011, 2013 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.dialogs;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.ILocatorModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;

/**
 * Peer selection dialog implementation.
 */
public class PeerSelectionDialog extends AbstractArraySelectionDialog implements ILocatorModelListener {

	/**
	 * Constructor.
	 *
	 * @param shell The shell used to view the dialog, or <code>null</code>.
	 */
	public PeerSelectionDialog(Shell shell) {
		super(shell, IContextHelpIds.PEER_SELECTION_DIALOG);

		ModelManager.getLocatorModel().addListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog#dispose()
	 */
	@Override
	protected void dispose() {
	    super.dispose();
		ModelManager.getLocatorModel().removeListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#getInput()
	 */
	@Override
    protected Object[] getInput() {
		Assert.isTrue(!Protocol.isDispatchThread());
		final ILocatorModelRefreshService service = ModelManager.getLocatorModel().getService(ILocatorModelRefreshService.class);
		if (service != null) {
			final Callback cb = new Callback();
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					service.refresh(cb);
				}
			});
			ExecutorsUtil.waitAndExecute(0, cb.getDoneConditionTester(null));
		}
		return ModelManager.getLocatorModel().getPeers();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#getDialogTitle()
	 */
	@Override
    protected String getDialogTitle() {
		return Messages.PeerSelectionDialog_dialogTitle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#getTitle()
	 */
	@Override
    protected String getTitle() {
		return Messages.PeerSelectionDialog_title;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#getDefaultMessage()
	 */
	@Override
    protected String getDefaultMessage() {
		return Messages.PeerSelectionDialog_message;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener#modelChanged(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel, org.eclipse.tcf.protocol.IPeer, boolean)
	 */
    @Override
    public void modelChanged(ILocatorModel model, IPeer peer, boolean added) {
    	refresh();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.ILocatorModelListener#modelDisposed(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.ILocatorModel)
	 */
    @Override
    public void modelDisposed(ILocatorModel model) {
    }
}
