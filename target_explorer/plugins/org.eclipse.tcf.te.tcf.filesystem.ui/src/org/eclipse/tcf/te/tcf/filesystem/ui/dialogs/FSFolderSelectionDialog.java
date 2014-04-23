/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.dialogs;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.runtime.callback.Callback;
import org.eclipse.tcf.te.tcf.filesystem.core.model.FSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.model.ModelManager;
import org.eclipse.tcf.te.tcf.filesystem.ui.activator.UIPlugin;
import org.eclipse.tcf.te.tcf.filesystem.ui.controls.FSTreeContentProvider;
import org.eclipse.tcf.te.tcf.filesystem.ui.controls.FSTreeViewerSorter;
import org.eclipse.tcf.te.tcf.filesystem.ui.interfaces.IFSConstants;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.ImageConsts;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.columns.FSTreeElementLabelProvider;
import org.eclipse.tcf.te.tcf.filesystem.ui.internal.handlers.MoveFilesHandler;
import org.eclipse.tcf.te.tcf.filesystem.ui.nls.Messages;
import org.eclipse.tcf.te.tcf.locator.interfaces.nodes.IPeerNode;
import org.eclipse.tcf.te.ui.trees.FilterDescriptor;
import org.eclipse.tcf.te.ui.trees.ViewerStateManager;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

/**
 * <p>
 * The folder selection dialog for a remote file system. To populate the tree of the selection
 * dialog with the file system, you should call <code>
 * ElementTreeSelectionDialog.setInput</code> to specify the peer model of the remote target. In
 * order to validate the destination folder, you should also specify the nodes to be moved. The file
 * selection dialog is of single selection. You can get the selected result by calling
 * <code>getFirstResult</code>. The type of selected folder is an instance of FSTreeNode.
 * </p>
 * <p>
 * The following is a snippet of example code:
 *
 * <pre>
 * FSFolderSelectionDialog dialog = new FSFolderSelectionDialog(shell);
 * dialog.setInput(peer);
 * dialog.setMovedNodes(nodes);
 * if (dialog.open() == Window.OK) {
 * 	Object obj = dialog.getFirstResult();
 * 	Assert.isTrue(obj instanceof FSTreeNode);
 * 	FSTreeNode folder = (FSTreeNode) obj;
 * 	// Use folder ...
 * }
 * </pre>
 *
 * @see MoveFilesHandler
 */
public class FSFolderSelectionDialog extends ElementTreeSelectionDialog {
	// The nodes that are being moved.
	private List<FSTreeNode> movedNodes;

	/**
	 * Create an FSFolderSelectionDialog using the specified shell as the parent.
	 *
	 * @param parentShell The parent shell.
	 */
	public FSFolderSelectionDialog(Shell parentShell) {
		this(parentShell, new FSTreeElementLabelProvider(), new FSTreeContentProvider());
	}

	/**
	 * Create an FSFolderSelectionDialog using the specified shell, an FSTreeLabelProvider, and a
	 * content provider that provides the tree nodes.
	 *
	 * @param parentShell The parent shell.
	 * @param labelProvider The label provider.
	 * @param contentProvider The content provider.
	 */
	private FSFolderSelectionDialog(Shell parentShell, ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
		super(parentShell, createDecoratingLabelProvider(labelProvider), contentProvider);
		setTitle(Messages.FSFolderSelectionDialog_MoveDialogTitle);
		setMessage(Messages.FSFolderSelectionDialog_MoveDialogMessage);
		this.setAllowMultiple(false);
		this.setComparator(new FSTreeViewerSorter());
		this.addFilter(new DirectoryFilter());
		this.setStatusLineAboveButtons(false);
		this.setValidator(new ISelectionStatusValidator() {

			@Override
			public IStatus validate(final Object[] selection) {
				getShell().getDisplay().asyncExec(new Runnable() {
					@SuppressWarnings("synthetic-access")
                    @Override
					public void run() {
						IStatus status = isValidFolder(selection);
						if (getTreeViewer().getSelection().equals(new StructuredSelection(selection))) {
							updateStatus(status);
						}
					}
				});
				return new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(), Messages.FSFolderSelectionDialog_validate_message);
			}
		});
	}

	/**
	 * The viewer filter used to filter out files.
	 */
	static class DirectoryFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof FSTreeNode) {
				FSTreeNode node = (FSTreeNode) element;
				if(node.isFile()) return false;
			}
			return true;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.ElementTreeSelectionDialog#setInput(java.lang.Object)
	 */
	@Override
	public void setInput(Object input) {
		super.setInput(input);
		FilterDescriptor[] filterDescriptors = ViewerStateManager.getInstance().getFilterDescriptors(IFSConstants.ID_TREE_VIEWER_FS, input);
		Assert.isNotNull(filterDescriptors);
		for (FilterDescriptor descriptor : filterDescriptors) {
			if (descriptor.isEnabled()) {
				addFilter(descriptor.getFilter());
			}
		}
	}

	/**
	 * Create a decorating label provider using the specified label provider.
	 *
	 * @param labelProvider The label provider that actually provides labels and images.
	 * @return The decorating label provider.
	 */
	private static ILabelProvider createDecoratingLabelProvider(ILabelProvider labelProvider) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IDecoratorManager manager = workbench.getDecoratorManager();
		ILabelDecorator decorator = manager.getLabelDecorator();
		return new DecoratingLabelProvider(labelProvider,decorator);
	}

	/**
	 * Set the nodes that are about to be moved.
	 *
	 * @param movedNodes The nodes.
	 */
	public void setMovedNodes(List<FSTreeNode> movedNodes) {
		this.movedNodes = movedNodes;
	}

	@Override
    public TreeViewer getTreeViewer() {
		return super.getTreeViewer();
	}

	/**
	 * Create the tree viewer and set it to the label provider.
	 */
	@Override
	protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
		TreeViewer viewer = super.doCreateTreeViewer(parent, style);
		viewer.getTree().addKeyListener(new KeyAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.KeyAdapter#keyReleased(org.eclipse.swt.events.KeyEvent)
			 */
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.F5) {
					refresh();
				}
			}
		});
		viewer.getTree().setLinesVisible(false);

		MenuManager menuMgr = new MenuManager();
	    menuMgr.setRemoveAllWhenShown(true);
	    menuMgr.addMenuListener(new IMenuListener() {
	        @Override
            public void menuAboutToShow(IMenuManager manager) {
	        	IAction action = new Action(Messages.FSFolderSelectionDialog_Refresh_menu, UIPlugin.getImageDescriptor(ImageConsts.REFRESH_IMAGE)) {
	            	@Override
	            	public void run() {
	            	    refresh();
	            	}
	            };
	            action.setAccelerator(SWT.F5);
	            manager.add(action);
	            manager.add(new Action(Messages.FSFolderSelectionDialog_RefreshAll_menu) {
	            	@Override
	            	public void run() {
	            	    refreshModel();
	            	}
	            });
	        }
	    });
	    Menu menu = menuMgr.createContextMenu(viewer.getControl());
	    viewer.getControl().setMenu(menu);

	    return viewer;
	}

	public void refresh() {
		ISelection sel = getTreeViewer().getSelection();
		if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
			Iterator<Object> it = ((IStructuredSelection)sel).iterator();
			while (it.hasNext()) {
				Object node = it.next();
				if (node instanceof FSTreeNode) {
					refreshNode((FSTreeNode)node);
				}
				else {
					refreshModel();
					return;
				}
			}
		}
		else {
			refreshModel();
		}
	}

	protected void refreshNode(final FSTreeNode treeNode) {
		treeNode.refresh(new Callback() {
			@Override
            protected void internalDone(Object caller, IStatus status) {
				getShell().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						getTreeViewer().refresh(treeNode, true);
					}
				});
			}
		});
	}

	protected void refreshModel() {
		Object input = getTreeViewer().getInput();
		if (input instanceof IPeerNode) {
			ModelManager.getRuntimeModel((IPeerNode)input).getRoot().refresh(new Callback() {
				@Override
				protected void internalDone(Object caller, IStatus status) {
					getShell().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							getTreeViewer().refresh(true);
						}
					});
				}
			});
		}
	}


	/**
	 * If the specified selection is a valid folder to be selected.
	 *
	 * @param selection The selected folders.
	 * @return An error status if it is invalid or an OK status indicating it is valid.
	 */
	IStatus isValidFolder(Object[] selection) {
		String pluginId = UIPlugin.getUniqueIdentifier();
		IStatus error = new Status(IStatus.ERROR, pluginId, null);
		if (selection == null || selection.length == 0) {
			return error;
		}
		if (!(selection[0] instanceof FSTreeNode)) {
			return error;
		}
		FSTreeNode target = (FSTreeNode) selection[0];
		if (movedNodes != null) {
			for (FSTreeNode node : movedNodes) {
				if (node == target || node.isAncestorOf(target)) {
					return error;
				}
			}
		}
		if(!target.isWritable()) {
			return error;
		}
		return new Status(IStatus.OK, pluginId, null);
	}
}
