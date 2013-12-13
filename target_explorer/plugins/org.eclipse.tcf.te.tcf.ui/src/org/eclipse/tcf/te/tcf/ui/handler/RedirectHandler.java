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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
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
import org.eclipse.tcf.te.tcf.locator.nodes.PeerRedirector;
import org.eclipse.tcf.te.tcf.ui.dialogs.RedirectPeerSelectionDialog;
import org.eclipse.tcf.te.tcf.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.swt.DisplayUtil;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.navigator.CommonNavigator;

/**
 * Redirect peer command handler implementation.
 */
public class RedirectHandler extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		// Determine the peer selected in Target Explorer tree
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			// Redirect is supporting single selection only
			Object candidate = ((IStructuredSelection)selection).getFirstElement();
			if (candidate instanceof IPeerNode) {
				final IPeerNode peerNode = (IPeerNode)candidate;

				// Create the agent selection dialog
				RedirectPeerSelectionDialog dialog = new RedirectPeerSelectionDialog(HandlerUtil.getActiveShell(event), null) {
					@Override
					protected void configureTableViewer(TableViewer viewer) {
						Assert.isNotNull(viewer);

						List<ViewerFilter> filter = new ArrayList<ViewerFilter>();
						if (viewer.getFilters() != null && viewer.getFilters().length > 0) {
							filter.addAll(Arrays.asList(viewer.getFilters()));
						}

						filter.add(new ViewerFilter() {
							@Override
							public boolean select(Viewer viewer, Object parentElement, Object element) {
								if (peerNode.equals(element)) {
									return false;
								}
								return true;
							}
						});

						viewer.setFilters(filter.toArray(new ViewerFilter[filter.size()]));
					}
				};

				// Open the dialog
				if (dialog.open() == Window.OK) {
					// Get the selected proxy from the dialog
					selection = dialog.getSelection();
					if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
						candidate = ((IStructuredSelection)selection).getFirstElement();
						if (candidate instanceof IPeerNode) {
							final IPeerNode proxy = (IPeerNode)candidate;

							Protocol.invokeLater(new Runnable() {
								@Override
								public void run() {
									redirect(peerNode, proxy);

									DisplayUtil.safeAsyncExec(new Runnable() {
										@Override
										public void run() {
											IWorkbenchPart part = HandlerUtil.getActivePart(event);
											if (part instanceof CommonNavigator) {
												CommonNavigator navigator = (CommonNavigator)part;
												navigator.selectReveal(new StructuredSelection(peerNode));
											}
										}
									});
								}
							});
						}
					}
				}
			}
		}


		return null;
	}

	/**
	 * Redirect the communication to the given peer through the given proxy.
	 * <p>
	 * The method must be called from within the TCF dispatch thread.
	 *
	 * @param peerNode The peer to redirect. Must not be <code>null</code>.
	 * @param proxy The proxy. Must not be <code>null</code>
	 */
	public void redirect(IPeerNode peerNode, IPeerNode proxy) {
		Assert.isNotNull(peerNode);
		Assert.isNotNull(proxy);
		Assert.isTrue(Protocol.isDispatchThread(), "Illegal Thread Access"); //$NON-NLS-1$

		// Get the peer attributes
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.putAll(peerNode.getPeer().getAttributes());
		// Set the redirection
		attributes.put(IPeerNodeProperties.PROP_REDIRECT_PROXY, proxy.getPeerId());

		try {
			IURIPersistenceService uRIPersistenceService = ServiceManager.getInstance().getService(IURIPersistenceService.class);
			if (uRIPersistenceService == null) {
				throw new IOException("Persistence service instance unavailable."); //$NON-NLS-1$
			}
			uRIPersistenceService.write(new Peer(attributes), null);

			// Create a peer redirector
			PeerRedirector redirector = new PeerRedirector(proxy.getPeer(), attributes);
			// And update the instance
			peerNode.setProperty(IPeerNodeProperties.PROP_INSTANCE, redirector);

			// Associate proxy (parent) and peer model (child)
			peerNode.setParent(proxy);
			Model.getModel().getService(IPeerModelUpdateService.class).addChild(peerNode);

			// Trigger a refresh of the locator model in a later dispatch cycle
			Protocol.invokeLater(new Runnable() {
				@Override
				public void run() {
					Model.getModel().getService(IPeerModelRefreshService.class).refresh(null);
				}
			});
		} catch (IOException e) {
			String template = NLS.bind(Messages.RedirectHandler_error_redirectFailed, Messages.PossibleCause);
			StatusHandlerUtil.handleStatus(StatusHelper.getStatus(e), peerNode, template,
											Messages.RedirectHandler_error_title, IContextHelpIds.MESSAGE_REDIRECT_FAILED, this, null);
		}
	}
}
