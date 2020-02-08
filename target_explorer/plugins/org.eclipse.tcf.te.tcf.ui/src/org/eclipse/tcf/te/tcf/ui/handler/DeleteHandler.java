/*******************************************************************************
 * Copyright (c) 2012, 2014 Wind River Systems, Inc. and others. All rights reserved.
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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.protocol.Protocol;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.runtime.events.EventManager;
import org.eclipse.tcf.te.runtime.interfaces.callback.ICallback;
import org.eclipse.tcf.te.runtime.persistence.interfaces.IURIPersistenceService;
import org.eclipse.tcf.te.runtime.services.ServiceManager;
import org.eclipse.tcf.te.runtime.services.ServiceUtils;
import org.eclipse.tcf.te.runtime.statushandler.StatusHandlerUtil;
import org.eclipse.tcf.te.runtime.utils.StatusHelper;
import org.eclipse.tcf.te.tcf.core.events.DeletedEvent;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNodeProperties;
import org.eclipse.tcf.te.tcf.locator.interfaces.services.IPeerModelRefreshService;
import org.eclipse.tcf.te.tcf.locator.model.ModelManager;
import org.eclipse.tcf.te.tcf.ui.help.IContextHelpIds;
import org.eclipse.tcf.te.tcf.ui.nls.Messages;
import org.eclipse.tcf.te.ui.interfaces.handler.IDeleteHandlerDelegate;
import org.eclipse.tcf.te.ui.views.Managers;
import org.eclipse.tcf.te.ui.views.ViewsUtil;
import org.eclipse.tcf.te.ui.views.interfaces.ICategory;
import org.eclipse.tcf.te.ui.views.interfaces.IUIConstants;
import org.eclipse.tcf.te.ui.views.interfaces.categories.ICategorizable;
import org.eclipse.tcf.te.ui.views.interfaces.categories.ICategoryManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Delete handler implementation.
 */
public class DeleteHandler extends AbstractHandler {
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
			delete(selection, new Callback() {
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
	 * Tests if this delete handler can delete the elements of the given
	 * selection.
	 *
	 * @param selection The selection. Must not be <code>null</code>.
	 * @return <code>True</code> if the selection can be deleted by this handler, <code>false</code> otherwise.
	 */
	public boolean canDelete(ISelection selection) {
		Assert.isNotNull(selection);

		boolean canDelete = false;

		if (!(selection instanceof ITreeSelection) && selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Iterator<?> it = ((IStructuredSelection)selection).iterator();
			List<TreePath> treePathes = new ArrayList<TreePath>();
			while (it.hasNext()) {
				Object sel = it.next();
				treePathes.add(new TreePath(new Object[]{sel}));
			}
			selection = new TreeSelection(treePathes.toArray(new TreePath[treePathes.size()]));
		}

		// The selection must be a tree selection and must not be empty
		if (selection instanceof ITreeSelection && !selection.isEmpty()) {
			// Assume the selection to be deletable
			canDelete = true;
			// Iterate the selection. All elements must be of type IPeerNode
			for (TreePath treePath : ((ITreeSelection)selection).getPaths()) {
				// Get the element
				Object element = treePath.getLastSegment();
				// This handler will take care of peer model nodes only
				if (!(element instanceof IPeerNode)) {
					canDelete = false;
					break;
				}

				// Check if there is a delete handler delegate for the element
				IDeleteHandlerDelegate delegate = ServiceUtils.getUIServiceDelegate(element, element, IDeleteHandlerDelegate.class);
				// If a delegate is available, ask the handler first if the given element is currently deletable
				if (delegate != null) canDelete = delegate.canDelete(treePath);

				if (!canDelete) {
					break;
				}
			}
		}

		return canDelete;
	}

	/**
	 * Internal helper class to describe the delete operation to perform.
	 */
	private static class Operation {
		// The operation types
		public enum TYPE { Remove, Unlink }

		// The parent delete handler
		public DeleteHandler parentHandler;
		// The element to operate on
		public IPeerNode node;
		// The operation type to perform
		public TYPE type;
		// In case of an "unlink" operation, the parent category
		// is required.
		public ICategory parentCategory;

		/**
		 * Constructor.
		 */
		public Operation() {
		}

		/**
		 * Executes the operation.
		 *
		 * @throws Exception if the operation fails.
		 */
		public void execute() throws Exception {
			Assert.isNotNull(node);
			Assert.isNotNull(type);

			if (TYPE.Remove.equals(type)) {
				// Remove the node from the persistence storage
				IURIPersistenceService service = ServiceManager.getInstance().getService(IURIPersistenceService.class);
				if (service == null) throw new IOException("Persistence service instance unavailable."); //$NON-NLS-1$
				service.delete(node, null);
				if (parentCategory != null) {
					ViewsUtil.setSelection(IUIConstants.ID_EXPLORER, new StructuredSelection(parentCategory));
				}

				// Mark the peer node as deleted
				Protocol.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						node.setProperty(IPeerNodeProperties.PROPERTY_IS_DELETED, true);
					}
				});

				// Check if there is a delete handler delegate for the element
				IDeleteHandlerDelegate delegate = ServiceUtils.getUIServiceDelegate(node, node, IDeleteHandlerDelegate.class);
				// If a delegate is available, signal the execution of the remove
				if (delegate != null) delegate.postDelete(node);

				// Send the peer node deleted event to also delete the launch configuration
				DeletedEvent event = new DeletedEvent(parentHandler, node);
				EventManager.getInstance().fireEvent(event);
			}
			else if (TYPE.Unlink.equals(type)) {
				Assert.isNotNull(parentCategory);

				ICategoryManager manager = Managers.getCategoryManager();
				Assert.isNotNull(manager);

				ICategorizable categorizable = (ICategorizable)node.getAdapter(ICategorizable.class);
				if (categorizable == null) {
					categorizable = (ICategorizable)Platform.getAdapterManager().getAdapter(node, ICategorizable.class);
				}
				Assert.isNotNull(categorizable);

				manager.remove(parentCategory.getId(), categorizable.getId());
				ViewsUtil.setSelection(IUIConstants.ID_EXPLORER, new StructuredSelection(parentCategory));
			}
		}
	}

	/**
	 * Deletes all elements from the given selection and invokes the
	 * given callback once done.
	 *
	 * @param selection The selection. Must not be <code>null</code>.
	 * @param callback The callback. Must not be <code>null</code>.
	 */
	public void delete(ISelection selection, final ICallback callback) {
		Assert.isNotNull(selection);
		Assert.isNotNull(callback);

		// The callback needs to be invoked in any case. However, if called
		// from an asynchronous callback, set this flag to false.
		boolean invokeCallback = true;

		if (!(selection instanceof ITreeSelection) && selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Iterator<?> it = ((IStructuredSelection)selection).iterator();
			List<TreePath> treePathes = new ArrayList<TreePath>();
			while (it.hasNext()) {
				Object sel = it.next();
				treePathes.add(new TreePath(new Object[]{sel}));
			}
			selection = new TreeSelection(treePathes.toArray(new TreePath[treePathes.size()]));
		}

		// The selection must be a tree selection and must not be empty
		if (selection instanceof ITreeSelection && !selection.isEmpty()) {
			// Determine the operations to perform for each of the selected elements
			Operation[] operations = selection2operations((ITreeSelection)selection);

			// Seek confirmation for the "remove" operations. If the user deny it,
			// everything, including the "unlink" operations are cancelled.
			boolean confirmed = confirmDelete(operations);

			// Execute the operations
			if (confirmed) {
				// If one of the operation is a "remove" operation, the locator
				// model needs to be refreshed
				boolean refreshModel = false;

				try {
					for (Operation op : operations) {
						if (Operation.TYPE.Remove.equals(op.type)) {
							refreshModel = true;
						}
						op.execute();
					}
				} catch (Exception e) {
					String template = NLS.bind(Messages.DeleteHandler_error_deleteFailed, Messages.PossibleCause);
					StatusHandlerUtil.handleStatus(StatusHelper.getStatus(e), selection, template,
													Messages.DeleteHandler_error_title, IContextHelpIds.MESSAGE_DELETE_FAILED, this, null);
				}

				if (refreshModel) {
					// Trigger a refresh of the model
					invokeCallback = false;
					Protocol.invokeLater(new Runnable() {
						@Override
						public void run() {
							IPeerModelRefreshService service = ModelManager.getPeerModel().getService(IPeerModelRefreshService.class);
							// Refresh the model now (must be executed within the TCF dispatch thread)
							if (service != null) service.refresh(new Callback() {
								@Override
								protected void internalDone(Object caller, IStatus status) {
									// Invoke the callback
									callback.done(DeleteHandler.this, Status.OK_STATUS);
								}
							});
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
	 * Analyze the given selection and convert it to an list of operations
	 * to perform.
	 *
	 * @param selection The selection. Must not be <code>null</code>.
	 * @return The list of operations.
	 */
	private Operation[] selection2operations(ITreeSelection selection) {
		Assert.isNotNull(selection);

		List<Operation> operations = new ArrayList<Operation>();

		// Iterate the selection. All elements must be of type IPeerNode
		for (TreePath treePath : selection.getPaths()) {
			// Get the element
			Object element = treePath.getLastSegment();
			Assert.isTrue(element instanceof IPeerNode);
			IPeerNode node = (IPeerNode)element;
			ICategory category = treePath.getFirstSegment() instanceof ICategory ? (ICategory)treePath.getFirstSegment() : null;

			Operation op = new Operation();
			if (category != null && IUIConstants.ID_CAT_FAVORITES.equals(category.getId())) {
				op.type = Operation.TYPE.Unlink;
			}
			else {
				op.type = Operation.TYPE.Remove;
			}
			op.node = node;
			op.parentCategory = category;
			op.parentHandler = this;
			operations.add(op);
		}

		return operations.toArray(new Operation[operations.size()]);
	}

	/**
	 * Confirm the deletion with the user.
	 *
	 * @param state The state of delegation handler.
	 * @return true if the user agrees to delete or it has confirmed previously.
	 */
	private boolean confirmDelete(Operation[] operations) {
		Assert.isNotNull(operations);

		boolean confirmed = false;

		// Find all elements to remove
		List<Operation> toRemove = new ArrayList<Operation>();
		for (Operation op : operations) {
			if (Operation.TYPE.Remove.equals(op.type)) {
				toRemove.add(op);
			}
		}

		// If there are node to remove -> ask for confirmation
		if (!toRemove.isEmpty()) {
			String question = getConfirmQuestion(toRemove);
			Shell parent = shell != null ? shell : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			confirmed = MessageDialog.openQuestion(parent, Messages.DeleteHandlerDelegate_DialogTitle, question);
		} else {
			confirmed = true;
		}

		return confirmed;
	}

	/**
	 * Get confirmation question displayed in the confirmation dialog.
	 *
	 * @param toRemove The list of nodes to remove.
	 * @return The question to ask the user.
	 */
	private String getConfirmQuestion(List<Operation> toRemove) {
		Assert.isNotNull(toRemove);

		String question;
		if (toRemove.size() == 1) {
			final Operation op = toRemove.get(0);
			final AtomicReference<String> name = new AtomicReference<String>();

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					name.set(op.node.getPeer().getName());
				}
			};

			if (Protocol.isDispatchThread()) {
				runnable.run();
			}
			else {
				Protocol.invokeAndWait(runnable);
			}

			question = NLS.bind(Messages.DeleteHandlerDelegate_MsgDeleteOnePeer, name.get());
		}
		else {
			question = NLS.bind(Messages.DeleteHandlerDelegate_MsgDeleteMultiplePeers, Integer.valueOf(toRemove.size()));
		}
		return question;
	}
}
