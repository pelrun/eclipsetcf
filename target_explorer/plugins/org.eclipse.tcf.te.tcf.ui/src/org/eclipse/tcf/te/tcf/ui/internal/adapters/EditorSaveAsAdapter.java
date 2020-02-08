/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.tcf.te.tcf.ui.internal.adapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.concurrent.util.ExecutorsUtil;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistableNodeProperties;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.tcf.core.peers.Peer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelLookupService;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.dialogs.RenameDialog;
import org.eclipse.tcf.te.ui.views.ViewsUtil;
import org.eclipse.tcf.te.ui.views.interfaces.IEditorSaveAsAdapter;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;

/**
 * EditorSaveAsAdapter
 */
public class EditorSaveAsAdapter implements IEditorSaveAsAdapter {

	/**
	 * Constructor.
	 */
	public EditorSaveAsAdapter() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.interfaces.IEditorSaveAsAdapter#isSaveAsAllowed(org.eclipse.ui.IEditorInput)
	 */
	@Override
	public boolean isSaveAsAllowed(IEditorInput input) {
		IPeerNode peerNode = (IPeerNode)input.getAdapter(IPeerNode.class);
		if (peerNode != null && peerNode.getConnectState() == IConnectable.STATE_DISCONNECTED) return true;

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tcf.te.ui.views.interfaces.IEditorSaveAsAdapter#doSaveAs(org.eclipse.ui.IEditorInput)
	 */
	@Override
	public Object doSaveAs(IEditorInput input) {
		IPeerNode peerNode = (IPeerNode)input.getAdapter(IPeerNode.class);
		if (peerNode != null) {

			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

			// Create the peer attributes
			final Map<String, String> attrs = new HashMap<String, String>(peerNode.getPeer().getAttributes());
			attrs.put(IPeer.ATTR_ID, UUID.randomUUID().toString());

			final List<String> usedNames = getUsedNameList();
			String title = Messages.EditorSaveAsAdapter_title;
			String prompt = Messages.EditorSaveAsAdapter_message;
			String usedError = Messages.EditorSaveAsAdapter_nameInUse_error;
			String label = Messages.EditorSaveAsAdapter_label;

			RenameDialog dialog = new RenameDialog(shell, title, null, prompt, usedError, null, label, attrs.get(IPeer.ATTR_NAME), null, usedNames.toArray(new String[usedNames.size()]), null);

			if (dialog.open() != Window.OK) {
				return null;
			}

			attrs.put(IPeer.ATTR_NAME, dialog.getNewName());
			attrs.remove(IPersistableNodeProperties.PROPERTY_URI);

			try {
				// Save the new peer
				IURIPersistenceService persistenceService = ServiceManager.getInstance().getService(IURIPersistenceService.class);
				if (persistenceService == null) {
					throw new IOException("Persistence service instance unavailable."); //$NON-NLS-1$
				}
				persistenceService.write(new Peer(attrs), null);

				final AtomicReference<IPeerNode> newPeer = new AtomicReference<IPeerNode>();
				final Callback cb = new Callback() {
					@Override
					protected void internalDone(Object caller, IStatus status) {
						// Get the peer model node from the model and select it in the tree
						IPeerNode peerNode = ModelManager.getPeerModel().getService(IPeerModelLookupService.class).lkupPeerModelById(attrs.get(IPeer.ATTR_ID));
						newPeer.set(peerNode);
						if (peerNode != null) {
							// Refresh the viewer
							ViewsUtil.refresh(IUIConstants.ID_EXPLORER);
							// Create the selection
							ISelection selection = new StructuredSelection(peerNode);
							// Set the selection
							ViewsUtil.setSelection(IUIConstants.ID_EXPLORER, selection);
							// And open the properties on the selection
						}
					}
				};
				// Trigger a refresh of the model to read in the newly created static peer
				Protocol.invokeLater(new Runnable() {
					@Override
					public void run() {
						IPeerModelRefreshService service = ModelManager.getPeerModel().getService(IPeerModelRefreshService.class);
						// Refresh the model now (must be executed within the TCF dispatch thread)
						if (service != null) {
							service.refresh(cb);
						}
					}
				});
				ExecutorsUtil.waitAndExecute(0, cb.getDoneConditionTester(null));
				return newPeer.get();
			} catch (IOException e) {
			}
		}
		return null;
	}

	/**
	 * Initialize the used name list.
	 */
	protected List<String> getUsedNameList() {
		final List<String> usedNames = new ArrayList<String>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				// Get all peer model objects
				IPeerNode[] peers = ModelManager.getPeerModel().getPeerNodes();
				// Loop them and find the ones which are of our handled types
				for (IPeerNode peerNode : peers) {
						String name = peerNode.getPeer().getName();
						Assert.isNotNull(name);
						if (!"".equals(name) && !usedNames.contains(name)) { //$NON-NLS-1$
							usedNames.add(name.trim().toUpperCase());
						}
				}
			}
		};

		if (Protocol.isDispatchThread()) {
			runnable.run();
		}
		else {
			Protocol.invokeAndWait(runnable);
		}

		return usedNames;
	}
}
