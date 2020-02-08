/*******************************************************************************
 * Copyright (c) 2012, 2016 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.ui.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.protocol.IPeer;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.core.interfaces.IConnectable;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IPersistableNodeProperties;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.statushandler.StatusHandlerUtil;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.core.peers.Peer;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerModel;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.dialogs.RenameDialog;
import org.eclipse.tcf.te.ui.views.ViewsUtil;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Rename handler implementation.
 */
public class RenameHandler extends AbstractHandler {
	// Remember the shell from the execution event
	private Shell shell = null;

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Get the shell
		shell = HandlerUtil.getActiveShell(event);
		// Get the current selection
		ISelection selection = getSelection(event);
		// Delete the selection
		if (selection != null) {
			rename(selection, new Callback() {
				@Override
				protected void internalDone(Object caller, IStatus status) {
					// Refresh the view
					ViewsUtil.refresh(IUIConstants.ID_EXPLORER);
				}
			});
		}
		// Reset the shell
		shell = null;

		return null;
	}

	protected ISelection getSelection(ExecutionEvent event) {
		return HandlerUtil.getCurrentSelection(event);
	}

	/**
	 * Renames all elements from the given selection and invokes the
	 * given callback once done.
	 *
	 * @param selection The selection. Must not be <code>null</code>.
	 * @param callback The callback. Must not be <code>null</code>.
	 */
	public void rename(final ISelection selection, final ICallback callback) {
		Assert.isNotNull(selection);
		Assert.isNotNull(callback);

		// The callback needs to be invoked in any case. However, if called
		// from an asynchronous callback, set this flag to false.
		boolean invokeCallback = true;

		// The selection must be a structured selection and must not be empty
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Iterator<?> iterator = ((IStructuredSelection)selection).iterator();
			while (iterator.hasNext()) {
				Object element = iterator.next();
				Assert.isTrue(element instanceof IPeerNode);
				final IPeerNode node = (IPeerNode)element;

				RenameDialog dialog = createRenameDialog(shell, node);
				int ok = dialog.open();
				if (ok == Window.OK) {
					// Do the renaming.
					final String newName = dialog.getNewName();

					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							if (newName != null && !"".equals(newName) && !newName.equals(node.getPeer().getName())) { //$NON-NLS-1$
								// To update the peer attributes, the peer needs to be recreated
								IPeer oldPeer = node.getPeer();
								// Create a write able copy of the peer attributes
								Map<String, String> attributes = new HashMap<String, String>(oldPeer.getAttributes());
								// Update the name
								attributes.put(IPeer.ATTR_NAME, newName);
								// Remove the persistence storage URI (if set)
								attributes.remove(IPersistableNodeProperties.PROPERTY_URI);
								// Create the new peer
								IPeer newPeer = new Peer(attributes);

								try {
									// Get the persistence service
									IURIPersistenceService uRIPersistenceService = ServiceManager.getInstance().getService(IURIPersistenceService.class);
									if (uRIPersistenceService == null) {
										throw new IOException("Persistence service instance unavailable."); //$NON-NLS-1$
									}

									// Update the peer node instance (silently)
									boolean changed = node.setChangeEventsEnabled(false);
									node.setProperty(IPeerNodeProperties.PROPERTY_INSTANCE, newPeer);
									if (changed) {
										node.setChangeEventsEnabled(true);
									}

									// Remove the old persisted peer
									uRIPersistenceService.delete(oldPeer, null);
									// Save the peer node to the new persistence storage
									uRIPersistenceService.write(newPeer, null);

									Protocol.invokeLater(new Runnable() {
										@Override
										public void run() {
											// Trigger a change event for the node
											node.fireChangeEvent("properties", null, node.getProperties()); //$NON-NLS-1$
										}
									});

								} catch (IOException e) {
									// Remove new peer and restore the old one
									IURIPersistenceService uRIPersistenceService = ServiceManager.getInstance().getService(IURIPersistenceService.class);
									if (uRIPersistenceService != null) {
										try {
											uRIPersistenceService.delete(newPeer, null);
										} catch (Exception ex) { /* Ignored on purpose */ }
										try {
											boolean changed = node.setChangeEventsEnabled(false);
											node.setProperty(IPeerNodeProperties.PROPERTY_INSTANCE, oldPeer);
											if (changed) {
												node.setChangeEventsEnabled(true);
											}
											Protocol.invokeLater(new Runnable() {
												@Override
												public void run() {
													// Trigger a change event for the node
													node.fireChangeEvent("properties", null, node.getProperties()); //$NON-NLS-1$
												}
											});
											uRIPersistenceService.write(oldPeer, null);
										} catch (Exception ex) { /* Ignored on purpose */ }
									}

									String template = NLS.bind(Messages.RenameHandler_error_renameFailed, Messages.PossibleCause);
									StatusHandlerUtil.handleStatus(StatusHelper.getStatus(e), selection, template,
													Messages.RenameHandler_error_title, IContextHelpIds.MESSAGE_RENAME_FAILED, this, null, true);
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

					// Trigger a refresh of the model
					invokeCallback = false;
					Protocol.invokeLater(new Runnable() {
						@Override
						public void run() {
							final IPeerModelRefreshService service = ModelManager.getPeerModel().getService(IPeerModelRefreshService.class);
							// Refresh the model now (must be executed within the TCF dispatch thread)
							if (service != null) {
								service.refresh(new Callback() {
									@Override
									protected void internalDone(Object caller, IStatus status) {
										// Invoke the callback
										callback.done(RenameHandler.this, Status.OK_STATUS);
									}
								});
							}
						}
					});
				}
			}
		}

		if (invokeCallback) {
			callback.done(this, Status.OK_STATUS);
		}
	}

	/**
	 * Create a renaming dialog for the specified peer model node.
	 *
	 * @param shell The parent shell or <code>null</code>
	 * @param node The peer model. Must not be <code>null</code>.
	 *
	 * @return The renaming dialog.
	 */
	private RenameDialog createRenameDialog(final Shell shell, final IPeerNode node) {
		Assert.isNotNull(node);

		final AtomicReference<String> name = new AtomicReference<String>();
		final List<String> usedNames = new ArrayList<String>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				name.set(node.getPeer().getName());

				IPeerModel model = ModelManager.getPeerModel();
				Assert.isNotNull(model);
				IPeerNode[] peers = model.getPeerNodes();
				for (IPeerNode peer : peers) {
						String name = peer.getPeer().getName();
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

		String title = NLS.bind(Messages.RenameHandler_dialog_title, name.get());
		String prompt = Messages.RenameHandler_dialog_message;
		String usedError = Messages.RenameHandler_dialog_error_nameExist;
		String formatError = Messages.RenameHandler_dialog_error_nameFormat;
		String label = Messages.RenameHandler_dialog_promptNewName;

		return new RenameDialog(shell, null, title, prompt, usedError, formatError, label, name.get(), "[0-9a-zA-Z. _()-]+", usedNames.toArray(new String[usedNames.size()]), null); //$NON-NLS-1$
	}

	/**
	 * Tests if this rename handler can rename the elements of the given
	 * <code>selection</code>.
	 *
	 * @param selection The selection. Must not be <code>null</code>.
	 * @return <code>True</code> if the selection can be renamed by this handler, <code>false</code> otherwise.
	 */
	public boolean canRename(ISelection selection) {
		Assert.isNotNull(selection);

		boolean canRename = false;

		if (!(selection instanceof ITreeSelection) && selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Iterator<?> it = ((IStructuredSelection)selection).iterator();
			List<TreePath> treePathes = new ArrayList<TreePath>();
			while (it.hasNext()) {
				Object sel = it.next();
				treePathes.add(new TreePath(new Object[]{sel}));
			}
			selection = new TreeSelection(treePathes.toArray(new TreePath[treePathes.size()]));
		}

		// The selection must be a tree selection, must not be empty, and only a single element must be selected
		if (selection instanceof ITreeSelection && !selection.isEmpty() && ((ITreeSelection)selection).size() == 1) {
			TreePath treePath = ((ITreeSelection)selection).getPaths()[0];
			// Get the element
			Object element = treePath.getLastSegment();
			// This handler will take care of peer model nodes only
			if (element instanceof IPeerNode) {
				IPeerNode node = (IPeerNode) element;
				// It can be renamed only when not connected
				canRename = IConnectable.STATE_DISCONNECTED == node.getConnectState();
			}
		}

		return canRename;
	}
}
