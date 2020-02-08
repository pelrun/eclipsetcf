/*******************************************************************************
 * Copyright (c) 2011, 2015, 2017 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.tcf.te.tcf.filesystem.ui.dialogs;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IFSTreeNode;
import org.eclipse.tcf.te.tcf.filesystem.core.interfaces.runtime.IRuntimeModel;
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
 * <code>getFirstResult</code>. The type of selected folder is an instance of IFSTreeNode.
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
 * 	Assert.isTrue(obj instanceof IFSTreeNode);
 * 	IFSTreeNode folder = (IFSTreeNode) obj;
 * 	// Use folder ...
 * }
 * </pre>
 *
 * @see MoveFilesHandler
 */
public final class FSFolderSelectionDialog extends ElementTreeSelectionDialog {
	// The nodes that are being moved.
	private List<IFSTreeNode> movedNodes;
	private final int mode;
	private final Set<String> fDisabledFilters = new HashSet<String>();

	public static final int MODE_ALL = 0;
	public static final int MODE_ALL_WARNING_NOT_WRITABLE = 1;
	public static final int MODE_ONLY_WRITABLE = 2;

	/**
	 * Create an FSFolderSelectionDialog using the specified shell as the parent.
	 *
	 * @param parentShell The parent shell.
	 */
	public FSFolderSelectionDialog(Shell parentShell) {
		this(parentShell, MODE_ONLY_WRITABLE);
	}

	/**
	 * Create an FSFolderSelectionDialog using the specified shell as the parent.
	 *
	 * @param parentShell The parent shell.
	 * @param mode The mode of this dialog.
	 */
	public FSFolderSelectionDialog(Shell parentShell, int mode) {
		this(parentShell, new FSTreeElementLabelProvider(), new FSTreeContentProvider(), mode);
	}

	/**
	 * Create an FSFolderSelectionDialog using the specified shell, an FSTreeLabelProvider, and a
	 * content provider that provides the tree nodes.
	 *
	 * @param parentShell The parent shell.
	 * @param labelProvider The label provider.
	 * @param contentProvider The content provider.
	 * @param mode The mode of this dialog.
	 */
	private FSFolderSelectionDialog(Shell parentShell, ILabelProvider labelProvider, ITreeContentProvider contentProvider, int mode) {
		super(parentShell, createDecoratingLabelProvider(labelProvider), contentProvider);
		this.mode = mode;
		setTitle(Messages.FSFolderSelectionDialog_MoveDialogTitle);
		setMessage(Messages.FSFolderSelectionDialog_MoveDialogMessage);
		this.setAllowMultiple(false);
		this.setComparator(new FSTreeViewerSorter());
		this.addFilter(new DirectoryFilter());
		this.setStatusLineAboveButtons(true);
		this.setValidator(new ISelectionStatusValidator() {
			@Override
			public IStatus validate(final Object[] selection) {
				return isValidFolder(selection);
			}
		});
	}

	/**
	 * The viewer filter used to filter out files.
	 */
	static class DirectoryFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof IFSTreeNode) {
				IFSTreeNode node = (IFSTreeNode) element;
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
			if (descriptor.isEnabled() && !fDisabledFilters.contains(descriptor.getId())) {
				addFilter(descriptor.getFilter());
			}
		}
	}

	/**
	 * Disable the filter with given ID.
	 * <p>
	 * <strong>Note:</strong> Must be called before {@link #setInput}.
	 * </p>
	 * @param filterId  the filter ID
	 */
	public final void disableFilter(String filterId) {
		fDisabledFilters.add(filterId);
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
	public void setMovedNodes(List<IFSTreeNode> movedNodes) {
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
		final TreeViewer viewer = super.doCreateTreeViewer(parent, style);

		Button refreshAll = new Button(parent, SWT.PUSH);
		refreshAll.setText(Messages.FSFolderSelectionDialog_RefreshAll_menu);
		refreshAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshModel(viewer);
			}
		});

		viewer.getTree().setLinesVisible(false);

		createContextMenuRefresh(viewer);

	    return viewer;
	}

	public static void createContextMenuRefresh(final TreeViewer viewer) {
		viewer.getTree().addKeyListener(new KeyAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.KeyAdapter#keyReleased(org.eclipse.swt.events.KeyEvent)
			 */
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.F5) {
					refresh(viewer);
				}
			}
		});

		MenuManager menuMgr = new MenuManager();
	    menuMgr.setRemoveAllWhenShown(true);
	    menuMgr.addMenuListener(new IMenuListener() {
	        @Override
            public void menuAboutToShow(IMenuManager manager) {
	        	IAction action = new Action(Messages.FSFolderSelectionDialog_Refresh_menu, UIPlugin.getImageDescriptor(ImageConsts.REFRESH_IMAGE)) {
	            	@Override
	            	public void run() {
	            	    refresh(viewer);
	            	}
	            };
	            action.setAccelerator(SWT.F5);
	            manager.add(action);
	        }
	    });
	    Menu menu = menuMgr.createContextMenu(viewer.getControl());
	    viewer.getControl().setMenu(menu);
	}

	public static void refresh(TreeViewer viewer) {
		ISelection sel = viewer.getSelection();
		if (sel instanceof IStructuredSelection && !sel.isEmpty()) {
			Iterator<Object> it = ((IStructuredSelection)sel).iterator();
			while (it.hasNext()) {
				Object node = it.next();
				if (node instanceof IFSTreeNode) {
					refreshNode((IFSTreeNode)node);
				}
				else {
					refreshModel(viewer);
					return;
				}
			}
		}
		else {
			refreshModel(viewer);
		}
	}

	protected static void refreshNode(final IFSTreeNode treeNode) {
		treeNode.operationRefresh(true).runInJob(null);
	}

	protected static void refreshModel(TreeViewer viewer) {
		Object input = viewer.getInput();
		if (input instanceof IPeerNode) {
			IRuntimeModel rtm = ModelManager.getRuntimeModel((IPeerNode)input);
			if (rtm != null)
				refreshNode(rtm.getRoot());
		}
	}


	private final static IStatus error = new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(), null);
	private final static IStatus errorNotWritable = new Status(IStatus.ERROR, UIPlugin.getUniqueIdentifier(), Messages.FSFolderSelectionDialog_notWritable_error);
	private final static IStatus warningNotWritable = new Status(IStatus.WARNING, UIPlugin.getUniqueIdentifier(), Messages.FSFolderSelectionDialog_notWritable_warning);

	/**
	 * If the specified selection is a valid folder to be selected.
	 *
	 * @param selection The selected folders.
	 * @return An error status if it is invalid or an OK status indicating it is valid.
	 */
	IStatus isValidFolder(Object[] selection) {
		if (selection == null || selection.length == 0) {
			return error;
		}
		if (!(selection[0] instanceof IFSTreeNode)) {
			return error;
		}
		IFSTreeNode target = (IFSTreeNode) selection[0];
		if (movedNodes != null) {
			for (IFSTreeNode node : movedNodes) {
				if (node == target || node.isAncestorOf(target)) {
					return error;
				}
			}
		}
		if(mode != MODE_ALL && !target.isWritable()) {
			return mode == MODE_ONLY_WRITABLE ? errorNotWritable : warningNotWritable;
		}
		return Status.OK_STATUS;
	}
}
