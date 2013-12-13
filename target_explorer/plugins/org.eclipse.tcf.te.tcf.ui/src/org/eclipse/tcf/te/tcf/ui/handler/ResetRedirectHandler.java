/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.statushandler.StatusHandlerUtil;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.core.peers.Peer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelUpdateService;
import org.eclipse.tcf.te.tcf.locator.model.Model;
import org.eclipse.tcf.te.tcf.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Reset peer redirection command handler.
 */
public class ResetRedirectHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Determine the peer selected in Target Explorer tree
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			// Redirect is supporting single selection only
			Object candidate = ((IStructuredSelection)selection).getFirstElement();
			if (candidate instanceof IPeerNode) {
				final IPeerNode peerNode = (IPeerNode)candidate;

				Protocol.invokeLater(new Runnable() {
					@Override
					public void run() {
						resetRedirect(peerNode);
					}
				});
			}
		}

		return null;
	}

	/**
	 * Reset the communication redirection for the given peer.
	 * <p>
	 * The method must be called from within the TCF dispatch thread.
	 *
	 * @param peerNode The peer to reset. Must not be <code>null</code>.
	 */
	public void resetRedirect(IPeerNode peerNode) {
		Assert.isNotNull(peerNode);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// Get the peer attributes
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.putAll(peerNode.getPeer().getAttributes());
		// Redirection set?
		if (attributes.get(IPeerNodeProperties.PROP_REDIRECT_PROXY) != null) {
			// Remove the redirection
			attributes.remove(IPeerNodeProperties.PROP_REDIRECT_PROXY);

			try {
				// Save it
				IURIPersistenceService uRIPersistenceService = ServiceManager.getInstance().getService(IURIPersistenceService.class);
				if (uRIPersistenceService == null) {
					throw new IOException("Persistence service instance unavailable."); //$NON-NLS-1$
				}
				// Create a peer
				IPeer peer = new Peer(attributes);
				uRIPersistenceService.write(peer, null);

				// And update the instance
				peerNode.setProperty(IPeerNodeProperties.PROP_INSTANCE, peer);

				// Reset proxy (parent) and peer model (child) association
				Model.getModel().getService(IPeerModelUpdateService.class).removeChild(peerNode);
				peerNode.setParent(null);

				// Trigger a refresh of the locator model in a later dispatch cycle
				Protocol.invokeLater(new Runnable() {
					@Override
					public void run() {
						Model.getModel().getService(IPeerModelRefreshService.class).refresh(null);
					}
				});
			} catch (IOException e) {
				String template = NLS.bind(Messages.ResetRedirectHandler_error_resetRedirectFailed, Messages.PossibleCause);
				StatusHandlerUtil.handleStatus(StatusHelper.getStatus(e), peerNode, template,
												Messages.ResetRedirectHandler_error_title, IContextHelpIds.MESSAGE_RESET_REDIRECT_FAILED, this, null);
			}
		}
	}
}
