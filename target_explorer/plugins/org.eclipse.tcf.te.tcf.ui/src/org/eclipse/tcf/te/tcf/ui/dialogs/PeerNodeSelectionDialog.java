/*******************************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.tcf.core.interfaces.IPeerProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;

/**
 * Peer selection dialog implementation.
 */
public class PeerNodeSelectionDialog extends AbstractArraySelectionDialog implements IPeerModelListener {

	final String[] services;

	/**
	 * Constructor.
	 *
	 * @param shell The shell used to view the dialog, or <code>null</code>.
	 */
	public PeerNodeSelectionDialog(Shell shell) {
		this(shell, null);
	}

	/**
	 * Constructor.
	 *
	 * @param shell The shell used to view the dialog, or <code>null</code>.
	 */
	public PeerNodeSelectionDialog(Shell shell, String[] services) {
		super(shell, IContextHelpIds.PEER_NODE_SELECTION_DIALOG);

		this.services = services;

		ModelManager.getPeerModel().addListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.jface.dialogs.CustomTitleAreaDialog#dispose()
	 */
	@Override
	protected void dispose() {
	    super.dispose();
		ModelManager.getPeerModel().removeListener(this);
	}

	@Override
    protected Object[] getInput() {
		List<IPeerNode> peerNodes = new ArrayList<IPeerNode>();
	    for (final IPeerNode peerNode : ModelManager.getPeerModel().getPeerNodes()) {
	    	if (peerNode.isVisible() && (getType() == null || getType().equals(peerNode.getPeerType()))) {
	    		if (services != null && services.length > 0) {
	    			final AtomicBoolean hasServices = new AtomicBoolean();
	    			Protocol.invokeAndWait(new Runnable() {
						@Override
						public void run() {
			    			String offlineServices = peerNode.getStringProperty(IPeerProperties.PROP_OFFLINE_SERVICES);
			    			String remoteServices = peerNode.getStringProperty(IPeerNodeProperties.PROPERTY_REMOTE_SERVICES);
			    			List<String> offline = offlineServices != null ? Arrays.asList(offlineServices.split(",\\s*")) : Collections.EMPTY_LIST; //$NON-NLS-1$
			    			List<String> remote = remoteServices != null ? Arrays.asList(remoteServices.split(",\\s*")) : null; //$NON-NLS-1$
			    			boolean hasOfflineService = true;
			    			for (String service : services) {
				    			hasOfflineService &= (remote == null) ? offline.contains(service) : remote.contains(service);
				    			if (!hasOfflineService) {
				    				break;
				    			}
                            }
			    			hasServices.set(hasOfflineService);
						}
					});
	    			if (!hasServices.get()) {
	    				continue;
	    			}
	    		}
	    		peerNodes.add(peerNode);
	    	}
        }

	    return peerNodes.toArray();
	}

	/**
	 * Get the peer node type to filter.
	 * @return The peer type id or <code>null</code> for all peer nodes.
	 */
	protected String getType() {
		return null;
	}

	/**
	 * Returns the dialog title.
	 *
	 * @return The dialog title.
	 */
	@Override
    protected String getDialogTitle() {
		return Messages.PeerNodeSelectionDialog_dialogTitle;
	}

	/**
	 * Returns the title.
	 *
	 * @return The title.
	 */
	@Override
    protected String getTitle() {
		return Messages.PeerNodeSelectionDialog_title;
	}

	/**
	 * Returns the default message.
	 *
	 * @return The default message.
	 */
	@Override
    protected String getDefaultMessage() {
		return Messages.PeerNodeSelectionDialog_message;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener#modelChanged(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel, org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode, boolean)
	 */
    @Override
    public void modelChanged(IPeerModel model, IPeerNode peerNode, boolean added) {
    	refresh();
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.locator.interfaces.IPeerModelListener#modelDisposed(org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel)
	 */
    @Override
    public void modelDisposed(IPeerModel model) {
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#updateSelection(org.eclipse.jface.viewers.ISelection)
	 */
    @Override
    protected void updateSelection(ISelection selection) {
    }

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.tcf.ui.dialogs.AbstractArraySelectionDialog#createButtonAreaContent(org.eclipse.swt.widgets.Composite)
	 */
    @Override
    protected void createButtonAreaContent(Composite parent) {
    }
}
